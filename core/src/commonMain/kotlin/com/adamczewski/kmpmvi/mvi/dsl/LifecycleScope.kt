package com.adamczewski.kmpmvi.mvi.dsl

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState

public class LifecycleScope<Action: MviAction, State : MviState, Effect : MviEffect, Message : MviMessage> @PublishedApi internal constructor(
    container: BaseMviContainer<Action, State, Effect, Message>
) : BaseMviScope<Action, State, Effect, Message>(container) {

    public fun onInit(block: suspend () -> Unit) {
        container.onInit(block)
    }

    public fun onSubscribe(block: suspend () -> Unit) {
        container.onSubscribe(block)
    }

    public fun onUnsubscribe(block: suspend () -> Unit) {
        container.onUnsubscribe(block)
    }
}
