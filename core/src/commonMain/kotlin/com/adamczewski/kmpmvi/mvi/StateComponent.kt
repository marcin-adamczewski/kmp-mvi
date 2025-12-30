package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import kotlinx.coroutines.flow.StateFlow

interface StateComponent<Action : MviAction, State, Effects : MviEffect> {
    val currentState: StateFlow<State>
    val effects: EffectsHandler<Effects>
    fun submitAction(action: Action)
}
