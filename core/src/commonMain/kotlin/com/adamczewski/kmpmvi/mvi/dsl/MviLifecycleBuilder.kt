package com.adamczewski.kmpmvi.mvi.dsl

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.error.BaseErrorManager
import com.adamczewski.kmpmvi.mvi.error.MviError
import com.adamczewski.kmpmvi.mvi.error.observeError
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.progress.ProgressManager
import com.adamczewski.kmpmvi.mvi.progress.ProgressObservable
import kotlinx.coroutines.CoroutineScope

@MviDsl
public class MviLifecycleBuilder<State : MviState, Effect : MviEffect, Message : MviMessage> @PublishedApi internal constructor(
    @PublishedApi internal val container: BaseMviContainer<*, State, Effect, Message>
) {
    public val scope: CoroutineScope get() = container.scope

    public val progress: ProgressManager get() = container.progress

    public fun onInit(block: suspend () -> Unit) {
        container.onInit(block)
    }

    public fun onSubscribe(block: suspend () -> Unit) {
        container.onSubscribe(block)
    }

    public fun onUnsubscribe(block: suspend () -> Unit) {
        container.onUnsubscribe(block)
    }

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
        container.messenger.setMessage(reducer(container.observableState.value))
    }

    public suspend fun <T> withProgress(block: suspend () -> T): T {
        return container.withProgress(block)
    }

    public fun observeProgress(
        progressObservable: ProgressObservable,
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit
    ) {
        container.observeProgress(progressObservable, block)
    }

    public fun observeProgress(block: suspend CoroutineScope.(showProgress: Boolean) -> Unit) {
        container.observeProgress(block)
    }

    public fun <E : MviError> observeError(
        errorManager: BaseErrorManager<E>,
        block: suspend CoroutineScope.(E?) -> Unit
    ) {
        container.observeError(errorManager, block)
    }
}
