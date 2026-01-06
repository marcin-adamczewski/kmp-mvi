@file:OptIn(ExperimentalForInheritanceCoroutinesApi::class)

package com.adamczewski.kmpmvi.mvi.state

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal class SubscriberCountStateFlow<T>(
    private val upstream: StateFlow<T>
) : StateFlow<T> {
    private val _subscriptionCount = MutableStateFlow(0)
    val subscriptionCount: StateFlow<Int> = _subscriptionCount

    override val value: T get() = upstream.value
    override val replayCache: List<T> get() = upstream.replayCache

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        _subscriptionCount.update { it + 1 }
        try {
            upstream.collect(collector)
        } finally {
            _subscriptionCount.update { it - 1 }
        }
    }
}
