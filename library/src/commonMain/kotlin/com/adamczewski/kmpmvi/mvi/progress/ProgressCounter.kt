@file:OptIn(ExperimentalUuidApi::class)

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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface ProgressObservable {
    val observeState: Flow<Boolean>
}

class ProgressCounter : ProgressObservable {
    // Initial value is -1 so we can easily filter it out as the initial state should be defined
    // in UI state.
    private val initialValue = -1
    private val progressState = MutableStateFlow(initialValue)
    private var progressIds = AtomicMutableSet<String>()

    override val observeState: Flow<Boolean> =
        progressState
            .filter { it != initialValue }
            .map { it > 0 }
            .distinctUntilChanged()

    // coercedAtLeast is used to be able to increment over an initial negative value
    fun addProgress(id: String) {
        if (progressIds.add(id)) {
            progressState.update { it.coerceAtLeast(0) + 1 }
        }
    }

    fun removeProgress(id: String) {
        if (progressIds.remove(id)) {
            progressState.update { (it - 1).coerceAtLeast(0) }
        }
    }

    /**
     * We have to pass [refreshId] so we don't remove progress of other refreshing or other
     * pending progress.
     */
    fun setRefreshing(refresh: Boolean, refreshId: String) {
        if (refresh) {
            addProgress(refreshId)
        } else {
            removeProgress(refreshId)
        }
    }
}

private fun generateId(): String = Uuid.random().toString()

/**
 * Shows progress immediately on start, and hides in either on error or on the first value.
 * Other value events are ignored and won't decrease counter as we don't want
 * to accidentally decrease other pending progresses counter, e.g. when we fetch values in
 * cacheAndFresh manner, when two values are emitted from this Flow.
 */
fun <T> Flow<T>.watchProgress(counter: ProgressCounter): Flow<T> {
    val id = generateId()
    return onStart { counter.addProgress(id) }
        .onEach { counter.removeProgress(id) }
        .onCompletion { counter.removeProgress(id) }
}

fun <T> SharedFlow<T>.watchProgress(counter: ProgressCounter): Flow<T> {
    val id = generateId()
    return onSubscription { counter.addProgress(id) }
        .onEach { counter.removeProgress(id) }
        .onCompletion { counter.removeProgress(id) }
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


suspend fun <T> withProgress(
    progressCounter: ProgressCounter,
    block: suspend () -> T,
): T {
    val id = generateId()
    progressCounter.addProgress(id)
    try {
        return block()
    } finally {
        progressCounter.removeProgress(id)
    }
}
