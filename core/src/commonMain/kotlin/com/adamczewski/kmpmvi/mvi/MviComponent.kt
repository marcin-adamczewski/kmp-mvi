package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.progress.ProgressManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public interface MviComponent<Action : MviAction, State : MviState, Effects : MviEffect, Message: MviMessage> {
    /**
     * StateFlow that emits current state of the component.
     * Should be only subscribed in lifecycle aware components, e.g. in UI
     */
    public val lifecycleState: StateFlow<State>

    /**
     * StateFlow that emits current state of the component.
     * Can be safely used in ViewModels and StateManager as subscribing to it
     * doesn't impact lifecycle callbacks.
     */
    public val observableState: StateFlow<State>

    /**
     * EffectsHandler that allows consuming effects emitted by the component.
     * It also allows to observer effetcs without consuming them.
     */
    public val effects: EffectsHandler<Effects>

    /**
     * Flow that emits messages. Messages can be useful for communication between MviComponents.
     */
    public val messages: Flow<Message>

    /**
     * Submits action to the component. Usually called from UI on clicks,
     * toggles, text changes, etc.
     */
    public fun submitAction(action: Action)
}
