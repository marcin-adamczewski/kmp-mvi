package com.adamczewski.kmpmvi.mvi.progress

import com.adamczewski.kmpmvi.mvi.utils.AtomicMutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlin.concurrent.atomics.AtomicBoolean

interface ProgressObservable {
    val observeState: Flow<Boolean>
}

class ProgressCounter : ProgressObservable {
    // Initial value is -1 so we can easily filter it out as the initial state should be defined
    // in UI state.
    private val initialValue = -1
    private val progressState = MutableStateFlow(initialValue)
    private var refreshingIds = AtomicMutableSet<String>()

    override val observeState: Flow<Boolean> =
        progressState
            .filter { it != initialValue }
            .map { it > 0 }
            .distinctUntilChanged()

    // coercedAtLeast is used to be able to increment over an initial negative value
    fun addProgress() {
        progressState.update {
            it.coerceAtLeast(0) + 1
        }
    }

    fun removeProgress() {
        progressState.update {
            (it - 1).coerceAtLeast(0)
        }
    }

    /**
     * We have to pass [refreshId] so we don't remove progress of other refreshing or other
     * pending progress.
     */
    fun setRefreshing(refresh: Boolean, refreshId: String) {
        if (refresh) {
            if (refreshingIds.add(refreshId)) {
                addProgress()
            }
        } else {
            if (refreshingIds.remove(refreshId)) {
                removeProgress()
            }
        }
    }
}

/**
 * The same as [watchProgress] but additionally removes refreshing progress on any
 * value or error. Pass the same [refreshId] you used when showing the progress in
 * [ProgressCounter.setRefreshing] method.
 */
fun <T> Flow<T>.watchProgressAndRefreshing(
    counter: ProgressCounter,
    refreshId: String,
): Flow<T> {
    return watchProgress(counter)
        .onEach {
            counter.setRefreshing(false, refreshId)
        }
        .onCompletion {
            counter.setRefreshing(false, refreshId)
        }
}

/**
 * Shows progress immediately on start, and hides in either on error or on the first value.
 * Other value events are ignored and won't decrease counter as we don't want
 * to accidentally decrease other pending progresses counter, e.g. when we fetch values in
 * cacheAndFresh manner, when two values are emitted from this Flow.
 */
fun <T> Flow<T>.watchProgress(counter: ProgressCounter): Flow<T> {
    return onStart { counter.addProgress() }
        .removeProgressOnAny(counter)
}

fun <T> SharedFlow<T>.watchProgress(counter: ProgressCounter): Flow<T> {
    return onSubscription { counter.addProgress() }
        .removeProgressOnAny(counter)
}

private fun <T> Flow<T>.removeProgressOnAny(counter: ProgressCounter): Flow<T> {
    val decreased = AtomicBoolean(false)
    fun decreaseCounter() {
        if (decreased.compareAndSet(expectedValue = false, newValue = true)) {
            counter.removeProgress()
        }
    }
    return this.onEach { decreaseCounter() }
        .onCompletion { decreaseCounter() }
}

suspend fun <T> withProgress(
    progressCounter: ProgressCounter,
    block: suspend () -> T,
): T {
    progressCounter.addProgress()
    try {
        return block()
    } finally {
        progressCounter.removeProgress()
    }
}
