package com.adamczewski.kmpmvi.mvi.settings

import com.adamczewski.kmpmvi.mvi.logger.DefaultMviLogger
import com.adamczewski.kmpmvi.mvi.utils.ScopeProvider
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds

public object DefaultMviSettingsProvider : MviSettingsProvider {
    override fun provide(
        tag: String,
        klass: KClass<out Any>
    ): MviSettings = MviSettings(
        isLoggerEnabled = true,
        logger = { DefaultMviLogger(tag = tag) },
        effectsBufferSize = 10,
        exceptionHandler = null,
        scopeProvider = { ScopeProvider.createMviScope() },
        isActionsThrottleEnabled = true,
        actionThrottleDuration = 500.milliseconds
    )
}
