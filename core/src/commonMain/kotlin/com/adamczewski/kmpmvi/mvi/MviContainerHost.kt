package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import kotlinx.coroutines.flow.StateFlow

public interface MviContainerHost<Action : MviAction, State : MviState, Effect : MviEffect> :
    MviComponent<Action, State, Effect> {

    public val component: MviComponent<Action, State, Effect>

    override val lifecycleState: StateFlow<State>
        get() = component.lifecycleState

    override val observableState: StateFlow<State>
        get() = component.observableState

    override val effects: EffectsHandler<Effect>
        get() = component.effects

    override fun submitAction(action: Action) {
        component.submitAction(action)
    }
}
