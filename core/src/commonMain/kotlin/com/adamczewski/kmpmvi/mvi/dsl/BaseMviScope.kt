package com.adamczewski.kmpmvi.mvi.dsl

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.progress.ProgressManager
import kotlinx.coroutines.CoroutineScope

@MviDsl
open public class BaseMviScope<Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage>(
    @PublishedApi internal val container: BaseMviContainer<Action, State, Effect, Message>,
) {
    public val scope: CoroutineScope get() = container.scope

    public val progress: ProgressManager get() = container.progress

    public val state: State get() = container.observableState.value

    public fun setState(reducer: State.() -> State) {
        container.setState(reducer)
    }

    public inline fun <reified T : State> updateState(
        crossinline reducer: T.() -> State
    ) {
        container.updateState<T>(reducer)
    }

    public suspend fun setEffect(
        requireConsumer: Boolean = false,
        reducer: suspend State.() -> Effect
    ) {
        container.setEffect(requireConsumer, reducer)
    }

    public suspend fun setEffectIfActive(
        reducer: suspend State.() -> Effect
    ) {
        container.setEffect(requireConsumer = true, reducer)
    }

    public suspend fun setMessage(reducer: suspend State.() -> Message) {
        container.messenger.setMessage(reducer(state))
    }

    public suspend fun <T> withProgress(block: suspend () -> T): T {
        return container.withProgress(block)
    }
}
