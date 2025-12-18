package com.adamczewski.kmpmvi.mvi


internal fun StateComponent<out MviAction, out MviState, out MviEffect>.defaultSettings(): Settings {
    return Settings(
        logger = {
            MviGlobalSettings.loggerProvider(
                "${this::class.simpleName}@${this.hashCode().toHexString()}",
                this::class
            )
        },
        exceptionHandler = MviGlobalSettings.exceptionHandler,
        scopeProvider = MviGlobalSettings.scopeProvider,
        effectsBufferSize = MviGlobalSettings.effectsBufferSize
    )
}
