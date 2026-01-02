package com.adamczewski.kmpmvi.mvi.logger

import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState

public class DefaultMviLogger(public val tag: String) : MviLogger {
    override fun onInitialState(state: MviState) {
        log("[Initial State] - $state")
    }

    override fun onState(state: MviState) {
        log("[State] - $state")
    }

    override fun onEffect(effect: MviEffect) {
        log("[Effect] - $effect")
    }

    override fun onAction(action: MviAction) {
        log("[Action] - $action")
    }

    override fun onMessage(action: MviMessage) {
        log("[Message] - $action")
    }

    override fun onInit() {
        log("[Lifecycle] - onInit")
    }

    override fun onSubscribe() {
        log("[Lifecycle] - onSubscribe")
    }

    override fun onUnsubscribe() {
        log("[Lifecycle] - onUnsubscribe")
    }

    override fun onClear() {
        log("[Lifecycle] - onClear")
    }

    private fun log(message: String) = println(
        "MVI $tag: $message"
    )
}
