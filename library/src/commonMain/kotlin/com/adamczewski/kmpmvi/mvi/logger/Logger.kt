package com.adamczewski.kmpmvi.mvi.logger

import com.adamczewski.kmpmvi.mvi.MviState
import com.adamczewski.kmpmvi.mvi.MviAction
import com.adamczewski.kmpmvi.mvi.MviEffect

interface Logger {
    fun onInitialState(state: MviState) {}
    fun onState(state: MviState) {}
    fun onEffect(effect: MviEffect) {}
    fun onAction(action: MviAction) {}
    fun onInit() {}
    fun onSubscribe() {}
    fun onUnsubscribe() {}
    fun onClear() {}
}
