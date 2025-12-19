package com.adamczewski.kmpmvi.mvi

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
