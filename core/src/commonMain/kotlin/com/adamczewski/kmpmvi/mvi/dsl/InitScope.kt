package com.adamczewski.kmpmvi.mvi.dsl

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.error.BaseErrorManager
import com.adamczewski.kmpmvi.mvi.error.MviError
import com.adamczewski.kmpmvi.mvi.error.observeError
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.progress.ProgressManager
import com.adamczewski.kmpmvi.mvi.progress.ProgressObservable
import kotlinx.coroutines.CoroutineScope

public class InitScope<Action: MviAction, State : MviState, Effect : MviEffect, Message : MviMessage> @PublishedApi internal constructor(
    container: BaseMviContainer<Action, State, Effect, Message>
) : BaseMviScope<Action, State, Effect, Message>(container) {

    public fun onInit(block: suspend () -> Unit) {
        container.onInit(block)
    }
}
