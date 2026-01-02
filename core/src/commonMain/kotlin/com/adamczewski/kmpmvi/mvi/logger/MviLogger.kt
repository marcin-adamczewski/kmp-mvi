package com.adamczewski.kmpmvi.mvi.logger

import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage

public interface MviLogger : LifecycleLogger {
    public fun onInitialState(state: MviState)
    public fun onState(state: MviState)
    public fun onEffect(effect: MviEffect)
    public fun onAction(action: MviAction)
    public fun onMessage(message: MviMessage)
}
