package com.adamczewski.kmpmvi.mvi.settings

import com.adamczewski.kmpmvi.mvi.logger.MviLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

public class MviSettingsBuilder internal constructor(settings: MviSettings) {
    public var isLoggerEnabled: Boolean = settings.isLoggerEnabled
    public var logger: () -> MviLogger = settings.logger
    public var effectsBufferSize: Int = settings.effectsBufferSize
    public var exceptionHandler: CoroutineExceptionHandler? = settings.exceptionHandler
    public var scopeProvider: () -> CoroutineScope = settings.scopeProvider

    internal fun build(): MviSettings = MviSettings(
        isLoggerEnabled = isLoggerEnabled,
        logger = logger,
        effectsBufferSize = effectsBufferSize,
        exceptionHandler = exceptionHandler,
        scopeProvider = scopeProvider,
    )
}
