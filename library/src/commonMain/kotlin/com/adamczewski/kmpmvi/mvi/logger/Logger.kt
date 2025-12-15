package com.adamczewski.kmpmvi.mvi.logger

import com.adamczewski.kmpmvi.mvi.MVIState
import com.adamczewski.kmpmvi.mvi.MviAction
import com.adamczewski.kmpmvi.mvi.MviEffect

interface Logger {
    fun onInitialState(state: MVIState) {}
    fun onState(state: MVIState) {}
    fun onEffect(effect: MviEffect) {}
    fun onAction(action: MviAction) {}
    fun onCleared() {}
}
