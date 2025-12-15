package com.adamczewski.kmpmvi.mvi.error

class UiError(
    val durationMs: Long,
    val error: Throwable? = null,
    val action: UiErrorAction? = null,
    val isIndefinite: Boolean = false,
)

interface UiErrorAction
