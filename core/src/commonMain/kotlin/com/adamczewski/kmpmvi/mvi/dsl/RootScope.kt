package com.adamczewski.kmpmvi.mvi.dsl

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.error.BaseErrorManager
import com.adamczewski.kmpmvi.mvi.error.MviError
import com.adamczewski.kmpmvi.mvi.error.observeError
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.progress.ProgressObservable
import kotlinx.coroutines.CoroutineScope

public class RootScope<Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage>(
    container: BaseMviContainer<Action, State, Effect, Message>
) : BaseMviScope<Action, State, Effect, Message>(container) {

    public fun actions(block: ActionsScope<Action, State, Effect, Message>.() -> Unit) {
        container.handleActions {
            ActionsScope(container, this).block()
        }
    }

    public fun lifecycle(block: LifecycleScope<Action, State, Effect, Message>.() -> Unit) {
        LifecycleScope<Action, State, Effect, Message>(container).block()
    }

    public fun onInit(block: InitScope<Action, State, Effect, Message>.() -> Unit) {
        InitScope<Action, State, Effect, Message>(container).block()
    }

    public fun <E : MviError> observeError(
        errorManager: BaseErrorManager<E>,
        block: suspend CoroutineScope.(E?) -> Unit
    ) {
        container.observeError(errorManager, block)
    }

    public fun observeProgress(
        progressObservable: ProgressObservable,
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit
    ) {
        container.observeProgress(progressObservable, block)
    }

    public fun observeProgress(block: suspend CoroutineScope.(showProgress: Boolean) -> Unit) {
        container.observeProgress(block)
    }
}