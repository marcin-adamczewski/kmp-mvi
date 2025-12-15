package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.logger.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.reflect.KClass

object MviGlobalSettings {
    var exceptionHandler: CoroutineExceptionHandler? = null
    var loggerProvider: (tag: String, containerClass: KClass<out Any>) -> Logger =
        { _, _ -> object : Logger {} }
}
