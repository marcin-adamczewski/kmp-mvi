package com.adamczewski.kmpmvi.mvi.error

import com.adamczewski.kmpmvi.mvi.MviComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun MviComponent<*, *, *>.observeError(
    errorManager: ErrorManager,
    block: suspend CoroutineScope.(UiError?) -> Unit,
) {
    scope.launch {
        errorManager.errors.collect { error ->
            block(error)
        }
    }
}
