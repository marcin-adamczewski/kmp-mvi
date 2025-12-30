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

class ProgressManager : ProgressObservable {
    // Initial value is -1 so we can easily filter it out as the initial state should be defined
    // in UI state.
    private val initialValue = -1
    private val progressState = MutableStateFlow(initialValue)
    private var progressIds = AtomicMutableSet<String>()

    override val isLoading: Flow<Boolean> =
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
}

private fun generateId(): String = Uuid.random().toString()

/**
 * Shows progress immediately on start, and hides on error or on the first value.
 * Other value events are ignored and won't decrease counter as we don't want
 */
fun <T> Flow<T>.watchProgress(
    manager: ProgressManager,
    id: String = generateId()
): Flow<T> {
    return onStart { manager.addProgress(id) }
        .onEach { manager.removeProgress(id) }
        .onCompletion { manager.removeProgress(id) }
}

/**
 * Shows progress immediately on start, and hides on error or on the first value.
 * Other value events are ignored and won't decrease counter as we don't want
 */
fun <T> SharedFlow<T>.watchProgress(
    manager: ProgressManager,
    id: String = generateId()
): Flow<T> {
    return onSubscription { manager.addProgress(id) }
        .onEach { manager.removeProgress(id) }
        .onCompletion { manager.removeProgress(id) }
}

/**
 * Shows progress immediately, and hides after block execution.
 */
suspend fun <T> withProgress(
    manager: ProgressManager,
    id: String = generateId(),
    block: suspend () -> T,
): T {
    manager.addProgress(id)
    try {
        return block()
    } finally {
        manager.removeProgress(id)
    }
}
