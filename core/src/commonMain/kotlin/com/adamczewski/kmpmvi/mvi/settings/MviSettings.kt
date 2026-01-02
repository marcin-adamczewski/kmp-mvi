package com.adamczewski.kmpmvi.mvi.settings

import com.adamczewski.kmpmvi.mvi.logger.MviLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

public data class MviSettings(
    val logger: () -> MviLogger,
    val effectsBufferSize: Int,
    val exceptionHandler: CoroutineExceptionHandler?,
    val scopeProvider: () -> CoroutineScope,
)
