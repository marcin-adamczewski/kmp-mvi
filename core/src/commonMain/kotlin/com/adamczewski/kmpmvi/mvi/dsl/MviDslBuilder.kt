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

@MviDsl
public class MviDslBuilder<Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage> @PublishedApi internal constructor(
    @PublishedApi internal val container: BaseMviContainer<Action, State, Effect, Message>
) {
    public val scope: CoroutineScope get() = container.scope

    public val progress: ProgressManager get() = container.progress

    public fun actions(block: MviActionsBuilder<Action, State, Effect, Message>.() -> Unit) {
        container.handleActions {
            MviActionsBuilder(container, this).block()
        }
    }

    public fun lifecycle(block: MviLifecycleBuilder<State, Effect, Message>.() -> Unit) {
        MviLifecycleBuilder<State, Effect, Message>(container).block()
    }

    public fun onInit(block: MviInitScopeBuilder<State, Effect, Message>.() -> Unit) {
        MviInitScopeBuilder<State, Effect, Message>(container).block()
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