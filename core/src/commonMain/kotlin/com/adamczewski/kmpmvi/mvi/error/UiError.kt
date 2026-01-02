package com.adamczewski.kmpmvi.mvi.error

public class UiError(
    override val durationMs: Long,
    public val error: Throwable? = null,
    public val action: UiErrorAction? = null,
    public val isIndefinite: Boolean = false,
) : Error

public interface UiErrorAction

public const val LONG_ERROR_DURATION: Long = 5_000L
public const val SHORT_ERROR_DURATION: Long = 3_000L

public fun Throwable.toUiError(
    durationMs: Long = LONG_ERROR_DURATION,
    action: UiErrorAction? = null,
    isIndefinite: Boolean = false,
): UiError = UiError(
    durationMs = durationMs,
    error = this,
    action = action,
    isIndefinite = isIndefinite
)
