package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public typealias MviContainerHost<A, S, E> = BaseMviContainerHost<A, S, E, Nothing>

public interface BaseMviContainerHost<Action : MviAction, State : MviState, Effect : MviEffect, Messages: MviMessage> :
    MviComponent<Action, State, Effect, Messages> {

    public val component: MviComponent<Action, State, Effect, Messages>

    override val lifecycleState: StateFlow<State>
        get() = component.lifecycleState

    override val observableState: StateFlow<State>
        get() = component.observableState

    override val effects: EffectsHandler<Effect>
        get() = component.effects

    override val messages: Flow<Messages>
        get() = component.messages

    override fun submitAction(action: Action) {
        component.submitAction(action)
    }
}
