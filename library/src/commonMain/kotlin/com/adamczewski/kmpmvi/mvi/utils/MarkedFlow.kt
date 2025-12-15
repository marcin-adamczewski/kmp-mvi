package com.adamczewski.kmpmvi.mvi.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

class MarkedFlow<T>(private val delegate: Flow<T>) : Flow<T> {
    var collected: Boolean = false
    override suspend fun collect(collector: FlowCollector<T>) {
        collected = true
        delegate.collect(collector)
    }
}
