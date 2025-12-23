package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.error.ErrorManager
import com.adamczewski.kmpmvi.mvi.error.UiError
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.error.observeError
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.model.NoMessages
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

typealias MviStateManager<A, S, E> = BaseMviStateManager<A, S, E, NoMessages>

abstract class BaseMviStateManager<Action : MviAction, State : MviState, Effect : MviEffect, Message: MviMessage>(
    initialState: State,
    settings: MviSettings? = null,
    vararg closeables: Closeable = arrayOf(),
) : Closeable, StateComponent<Action, State, Effect> {
    private val closeables = mutableListOf(*closeables)

    protected val component = with(settings ?: defaultSettings()) {
        BaseMviComponent<Action, State, Effect, Message>(
            initialState = initialState,
            scopeProvider = scopeProvider,
            settings = this
        )
    }

    protected val scope = component.scope

    override val currentState: StateFlow<State> = component.currentState

    protected val stateValue: State
        get() = currentState.value

    override val effects: EffectsHandler<Effect> = component.effects

    val progress: ProgressCounter = component.progress

    val messages: Flow<Message> = component.messenger.messages

    init {
        component.handleActions {
            handleActions()
        }
    }

    /**
     * Override this method to handle actions.
     * It's recommended to call only methods from provided [ActionsManager] in this method.
     * Other methods should be called within actions handling functions.
     */
    protected abstract fun ActionsManager<Action>.handleActions()


    fun onInit(block: suspend () -> Unit) {
        component.onInit(block)
    }

    fun onSubscribe(block: suspend () -> Unit) {
        component.onSubscribe(block)
    }

    fun onUnsubscribe(block: suspend () -> Unit) {
        component.onUnsubscribe(block)
    }

    override fun close() {
        closeables.forEach { it.close() }
        component.clear()
    }

    final override fun submitAction(action: Action) {
        component.submitAction(action)
    }

    protected fun setState(reducer: State.() -> State) {
        component.setState(reducer)
    }

    protected suspend fun setEffect(
        requireConsumer: Boolean = false,
        reducer: suspend State.() -> Effect,
    ) {
        component.setEffect(requireConsumer, reducer)
    }

    protected suspend fun setEffectIfActive(
        reducer: suspend State.() -> Effect,
    ) {
        setEffect(requireConsumer = true, reducer)
    }

    protected suspend fun setMessage(reducer: suspend State.() -> Message) {
        component.messenger.setMessage(reducer(stateValue))
    }

    protected inline fun <reified M : MviMessage> onMessageFlow(
        child: BaseMviStateManager<*, *, *, in M>,
        noinline flowMapper: suspend Flow<M>.() -> Flow<*>,
    ) {
        component.messenger.onMessageFlow(M::class, child, flowMapper)
    }

    protected inline fun <reified M : MviMessage> onMessage(
        child: BaseMviStateManager<*, *, *, in M>,
        noinline block: suspend (M) -> Unit,
    ) {
        component.messenger.onMessage(M::class, child, block)
    }

    protected fun observeProgress(
        vararg progressObservables: ProgressObservable,
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit,
    ) {
        component.observeProgress(CombinedProgressPublisher(*progressObservables), block)
    }

    protected fun observeProgress(
        progressObservable: ProgressObservable,
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit,
    ) {
        component.observeProgress(progressObservable, block)
    }

    protected fun observeProgress(
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit,
    ) {
        component.observeProgress(block)
    }

    protected suspend fun <T> withProgress(block: suspend () -> T) =
        component.withProgress(block)

    protected fun observeError(
        errorManager: ErrorManager,
        block: suspend CoroutineScope.(UiError?) -> Unit,
    ) {
        component.observeError(errorManager, block)
    }

    protected fun addCloseable(closeable: Closeable) {
        closeables.add(closeable)
    }
}

interface Closeable {
    fun close()
}
