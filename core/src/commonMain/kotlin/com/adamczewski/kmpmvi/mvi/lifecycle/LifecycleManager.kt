package com.adamczewski.kmpmvi.mvi.lifecycle

import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.logger.LifecycleLogger
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take

public class LifecycleManager(
    stateSubscriptionsCount: StateFlow<Int>,
    private val scope: CoroutineScope,
    private val logger: LifecycleLogger
) {
    public val lifecycle: StateFlow<MviLifecycle> = stateSubscriptionsCount
        .drop(1) // We're not interested in initial 0 value to not call onUnsubscribe initially
        .map { subscriptionsCount ->
            if (subscriptionsCount > 0) MviLifecycle.SUBSCRIBED else MviLifecycle.UNSUBSCRIBED
        }
        .stateIn(scope, SharingStarted.Eagerly, MviLifecycle.IDLE)

    private val anySubscribe = lifecycle.filter { it == MviLifecycle.SUBSCRIBED }

    private val firstSubscribe = anySubscribe.take(1)

    private val anyUnsubscribe = lifecycle.filter { it == MviLifecycle.UNSUBSCRIBED }

    init {
        logLifecycle()
    }

    public fun onInit(block: suspend () -> Unit) {
        firstSubscribe
            .onEach { block() }
            .launchIn(scope)
    }

    public fun onSubscribe(block: suspend () -> Unit) {
        anySubscribe
            .onEach { block() }
            .launchIn(scope)
    }

    public fun onUnsubscribe(block: suspend () -> Unit) {
        anyUnsubscribe
            .onEach { block() }
            .launchIn(scope)
    }


    private fun logLifecycle() {
        onInit { logger.onInit() }
        onSubscribe { logger.onSubscribe() }
        onUnsubscribe { logger.onUnsubscribe() }
    }
}

public enum class MviLifecycle {
    IDLE, SUBSCRIBED, UNSUBSCRIBED
}
