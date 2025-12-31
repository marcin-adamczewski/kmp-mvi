package com.adamczewski.kmpmvi.mvi.error

class UiError(
    override val durationMs: Long,
    val error: Throwable? = null,
    val action: UiErrorAction? = null,
    val isIndefinite: Boolean = false,
) : Error

interface UiErrorAction

const val LONG_ERROR_DURATION = 5_000L
const val SHORT_ERROR_DURATION = 3_000L

fun Throwable.toUiError(
    durationMs: Long = LONG_ERROR_DURATION,
    action: UiErrorAction? = null,
    isIndefinite: Boolean = false,
) =
    UiError(
        durationMs = durationMs,
        error = this,
        action = action,
        isIndefinite = isIndefinite
    )
