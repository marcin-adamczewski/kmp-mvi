package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.logger.Logger
import com.adamczewski.kmpmvi.mvi.utils.ScopeProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

object MviGlobalSettings {
    var exceptionHandler: CoroutineExceptionHandler? = null
    var loggerProvider: (tag: String, containerClass: KClass<out Any>) -> Logger =
        { _, _ -> object : Logger {} }
    var scopeProvider: () -> CoroutineScope = { ScopeProvider.createViewModelScope() }
    var effectsBufferSize: Int = 10
}
