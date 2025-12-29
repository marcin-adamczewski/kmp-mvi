package com.adamczewski.kmpmvi.mvi.lifecycle

import com.adamczewski.kmpmvi.mvi.logger.LifecycleLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take

class LifecycleManager(
    stateSubscriptionsCount: StateFlow<Int>,
    private val scope: CoroutineScope,
    private val logger: LifecycleLogger
) {
    private val isStateSubscribed: StateFlow<Boolean?> = stateSubscriptionsCount
        .drop(2) // We're not interested in initial 0 value to not call onUnsubscribe initially, as well for initial logger subscriber
        .map { it >= MIN_SUBSCRIBERS_COUNT }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    fun onInit(block: suspend () -> Unit) {
        isStateSubscribed
            .filter { it == true }
            .take(1)
            .onEach {
                logger.onInit()
                block()
            }
            .launchIn(scope)
    }


    fun onSubscribe(block: suspend () -> Unit) {
        isStateSubscribed
            .filter { it == true }
            .onEach {
                logger.onSubscribe()
                block()
            }
            .launchIn(scope)
    }

    fun onUnsubscribe(block: suspend () -> Unit) {
        isStateSubscribed
            .filter { it == false }
            .onEach {
                logger.onUnsubscribe()
                block()
            }
            .launchIn(scope)
    }

    private companion object {
        // One subscriber is for a logger
        private const val MIN_SUBSCRIBERS_COUNT = 2
    }
}
