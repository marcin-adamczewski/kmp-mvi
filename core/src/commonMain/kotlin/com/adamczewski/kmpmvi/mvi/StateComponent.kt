package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import kotlinx.coroutines.flow.StateFlow

public interface StateComponent<Action : MviAction, State, Effects : MviEffect> {
    public val currentState: StateFlow<State>
    public val effects: EffectsHandler<Effects>
    public fun submitAction(action: Action)
}
