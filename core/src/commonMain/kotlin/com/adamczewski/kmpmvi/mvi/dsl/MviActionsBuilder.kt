package com.adamczewski.kmpmvi.mvi.dsl

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.progress.ProgressManager
import kotlinx.coroutines.flow.Flow

@MviDsl
public class MviActionsBuilder<Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage> @PublishedApi internal constructor(
    @PublishedApi internal val container: BaseMviContainer<Action, State, Effect, Message>,
    @PublishedApi internal val actionsManager: ActionsManager<Action>
) {
    public val progress: ProgressManager get() = container.progress

    public inline fun <reified T : Action> onAction(
        noinline block: suspend MviActionScope<State, Effect, Message>.(T) -> Unit
    ) {
        actionsManager.onAction<T> { action ->
            MviActionScope(container).block(action)
        }
    }

    public inline fun <reified T : Action> onActionFlow(
        noinline transformer: suspend Flow<T>.(MviActionScope<State, Effect, Message>) -> Flow<*>
    ) {
        actionsManager.onActionFlow<T> {
            transformer(MviActionScope(container))
        }
    }

    public inline fun <reified T : Action> onActionSingle(
        noinline block: suspend MviActionScope<State, Effect, Message>.(T) -> Unit
    ) {
        actionsManager.onActionSingle<T> { action ->
            MviActionScope(container).block(action)
        }
    }

    public inline fun <reified T : Action> onActionFlowSingle(
        noinline flow: suspend (T) -> Flow<*>
    ) {
        actionsManager.onActionFlowSingle<T>(flow)
    }
}
