package com.adamczewski.kmpmvi.mvi.error

import com.adamczewski.kmpmvi.mvi.BaseMviComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun BaseMviComponent<*, *, *, *>.observeError(
    errorManager: ErrorManager,
    block: suspend CoroutineScope.(UiError?) -> Unit,
) {
    scope.launch {
        errorManager.errors.collect { error ->
            block(error)
        }
    }
}
