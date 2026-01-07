package com.adamczewski.kmpmvi.mvi.lifecycle

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest

public interface LifecycleAware {
    public val lifecycleManager: LifecycleManager

    public suspend fun withLifecycle(
        block: suspend () -> Unit
    ) {
        coroutineScope {
            lifecycleManager.lifecycle
                .filter { it != MviLifecycle.IDLE }
                .mapLatest { lifecycle ->
                    if (lifecycle == MviLifecycle.SUBSCRIBED) {
                        block()
                    }
                }
                .launchIn(this)
        }
    }

    public fun <T : Any> Flow<T>.withLifecycle(): Flow<T> {
        return lifecycleManager.lifecycle
            .filter { it != MviLifecycle.IDLE }
            .flatMapLatest { lifecycle ->
                if (lifecycle == MviLifecycle.SUBSCRIBED) {
                    this
                } else {
                    emptyFlow()
                }
            }
    }
}
