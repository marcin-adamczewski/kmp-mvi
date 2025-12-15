package com.adamczewski.kmpmvi.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch

interface ProgressObservable {
    val observeState: Flow<Boolean>
}

class ProgressCounter : ProgressObservable {
    private val count = AtomicInt(0)
    private val progressState = MutableStateFlow(count.load())
    private var refreshingIds = mutableSetOf<String>()

    override val observeState: Flow<Boolean> =
        progressState.map { it > 0 }.drop(1).distinctUntilChanged()

    fun addProgress() {
        progressState.value = count.incrementAndFetch()
    }

    fun removeProgress() {
        progressState.value = count.decrementAndFetch()
    }

    /**
     * We have to pass [refreshId] so we don't remove progress of other refreshing or other
     * pending progress.
     */
    fun setRefreshing(refresh: Boolean, refreshId: String) {
        if (refresh) {
            refreshingIds.add(refreshId)
            addProgress()
        } else if (refreshingIds.contains(refreshId)) {
            refreshingIds.remove(refreshId)
            if (count.load() > 0) {
                removeProgress()
            }
        }
    }
}

class CombinedProgressPublisher(
    vararg progressObservables: ProgressObservable,
) : ProgressObservable {
    override val observeState: Flow<Boolean> =
        combine(progressObservables.map { it.observeState }) { progresses ->
            progresses.any { isInProgress -> isInProgress }
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
