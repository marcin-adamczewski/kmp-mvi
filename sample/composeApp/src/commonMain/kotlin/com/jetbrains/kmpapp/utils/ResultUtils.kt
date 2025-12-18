package com.jetbrains.kmpapp.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

fun <T> Flow<Result<T>>.onSuccess(action: suspend (T) -> Unit) : Flow<Result<T>> {
    return onEach { result ->
        result.onSuccess { action(it) }
    }
}

fun <T> Flow<Result<T>>.onError(action: suspend (Throwable) -> Unit) : Flow<Result<T>> {
    return onEach { result ->
        result.onFailure { action(it) }
    }
}
