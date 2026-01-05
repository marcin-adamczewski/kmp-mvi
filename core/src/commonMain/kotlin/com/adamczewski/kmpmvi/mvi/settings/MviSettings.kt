package com.adamczewski.kmpmvi.mvi.settings

import com.adamczewski.kmpmvi.mvi.logger.MviLogger
import com.adamczewski.kmpmvi.mvi.logger.NoOpLogger
import com.adamczewski.kmpmvi.mvi.utils.ScopeProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

public class MviSettings internal constructor(
    public val isLoggerEnabled: Boolean,
    public val logger: () -> MviLogger,
    public val effectsBufferSize: Int,
    public val exceptionHandler: CoroutineExceptionHandler?,
    public val scopeProvider: () -> CoroutineScope,
)

public fun buildMviSettings(
    baseSettings: MviSettings,
    block: MviSettingsBuilder.() -> Unit
): MviSettings = MviSettingsBuilder(baseSettings).apply(block).build()
