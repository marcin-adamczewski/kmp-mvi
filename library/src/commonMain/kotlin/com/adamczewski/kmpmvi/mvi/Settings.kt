package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.logger.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

class Settings(
    val logger: () -> Logger = { object : Logger {} },
    val effectsBufferSize: Int = MviGlobalSettings.effectsBufferSize,
    val exceptionHandler: CoroutineExceptionHandler? = MviGlobalSettings.exceptionHandler,
    val scopeProvider: () -> CoroutineScope = MviGlobalSettings.scopeProvider,
)
