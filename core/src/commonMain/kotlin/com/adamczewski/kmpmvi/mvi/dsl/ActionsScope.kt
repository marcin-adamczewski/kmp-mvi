package com.adamczewski.kmpmvi.mvi.dsl

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import kotlinx.coroutines.flow.Flow

@MviDsl
public class ActionsScope<Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage> @PublishedApi internal constructor(
    mviContainer: BaseMviContainer<Action, State, Effect, Message>,
    @PublishedApi internal val actionsManager: ActionsManager<Action>
) : BaseMviScope<Action, State, Effect, Message>(mviContainer) {

    public inline fun <reified T : Action> onAction(
        noinline block: suspend (T) -> Unit
    ) {
        actionsManager.onAction<T> { action ->
            block(action)
        }
    }

    public inline fun <reified T : Action> onActionFlow(
        noinline transformer: suspend Flow<T>.() -> Flow<*>
    ) {
        actionsManager.onActionFlow<T> {
            transformer(this)
        }
    }

    public inline fun <reified T : Action> onActionSingle(
        noinline block: suspend (T) -> Unit
    ) {
        actionsManager.onActionSingle<T> { action ->
            block(action)
        }
    }

    public inline fun <reified T : Action> onActionFlowSingle(
        noinline flow: suspend (T) -> Flow<*>
    ) {
        actionsManager.onActionFlowSingle<T>(flow)
    }
}
