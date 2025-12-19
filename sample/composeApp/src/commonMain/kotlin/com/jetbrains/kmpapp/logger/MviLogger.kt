package com.jetbrains.kmpapp.logger

import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.logger.Logger

class MviLogger(tag: String) : Logger {
    private val tag = "MVI $tag"

    override fun onInitialState(state: MviState) {
        super.onInitialState(state)
        println("$tag: $state")
    }

    override fun onAction(action: MviAction) {
        super.onAction(action)
        println("$tag: $action")
    }

    override fun onState(state: MviState) {
        super.onState(state)
        println("$tag: $state")
    }

    override fun onEffect(effect: MviEffect) {
        super.onEffect(effect)
        println("$tag: $effect")
    }

    override fun onInit() {
        super.onInit()
        println("$tag: onInit")
    }

    override fun onClear() {
        super.onClear()
        println("$tag: onClear")
    }
}
