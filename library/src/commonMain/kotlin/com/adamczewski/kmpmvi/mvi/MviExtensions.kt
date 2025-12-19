package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState

internal fun StateComponent<out MviAction, out MviState, out MviEffect>.defaultSettings(): Settings {
    val klass = this::class
    val tag = "${klass.simpleName}@${this.hashCode().toHexString()}"
    return Settings(
        logger = {
            MviGlobalSettings.loggerProvider(tag, klass)
        },
        exceptionHandler = MviGlobalSettings.exceptionHandler,
        scopeProvider = MviGlobalSettings.scopeProvider,
        effectsBufferSize = MviGlobalSettings.effectsBufferSize
    )
}
