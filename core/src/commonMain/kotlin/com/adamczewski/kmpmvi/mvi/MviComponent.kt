package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import kotlinx.coroutines.flow.StateFlow

public interface MviComponent<Action : MviAction, State, Effects : MviEffect> {
    public val state: StateFlow<State>
    public val observableState: StateFlow<State>
    public val effects: EffectsHandler<Effects>
    public fun submitAction(action: Action)
}
