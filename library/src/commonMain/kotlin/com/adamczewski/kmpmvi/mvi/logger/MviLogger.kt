package com.adamczewski.kmpmvi.mvi.logger

import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage

interface MviLogger : LifecycleLogger {
    fun onInitialState(state: MviState)
    fun onState(state: MviState)
    fun onEffect(effect: MviEffect)
    fun onAction(action: MviAction)
    fun onMessage(action: MviMessage)
}
