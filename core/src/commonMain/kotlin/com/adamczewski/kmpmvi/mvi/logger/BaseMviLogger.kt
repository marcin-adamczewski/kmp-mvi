package com.adamczewski.kmpmvi.mvi.logger

import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState

open class BaseMviLogger : MviLogger {
    override fun onInitialState(state: MviState) {}
    override fun onState(state: MviState) {}
    override fun onEffect(effect: MviEffect) {}
    override fun onAction(action: MviAction) {}
    override fun onMessage(action: MviMessage) {}
    override fun onInit() {}
    override fun onSubscribe() {}
    override fun onUnsubscribe() {}
    override fun onClear() {}
}
