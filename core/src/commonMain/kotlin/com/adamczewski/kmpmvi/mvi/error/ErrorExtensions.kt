package com.adamczewski.kmpmvi.mvi.error

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

public fun <E: Error> BaseMviContainer<*, *, *, *>.observeError(
    errorManager: BaseErrorManager<E>,
    block: suspend CoroutineScope.(E?) -> Unit,
) {
    scope.launch {
        errorManager.errors.collect { error ->
            block(error)
        }
    }
}
