@file:OptIn(ExperimentalTime::class)

package com.adamczewski.kmpmvi.mvi.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

public fun <T> Flow<T>.throttleFirst(window: Duration): Flow<T> = channelFlow {
    var lastEmissionTime = 0L
    val windowMillis = window.inWholeMilliseconds

    collect { value ->
        val currentTime = Clock.System.now().toEpochMilliseconds()
        if (currentTime - lastEmissionTime >= windowMillis) {
            lastEmissionTime = currentTime
            trySend(value)
        }
    }
}
