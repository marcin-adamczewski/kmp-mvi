package com.adamczewski.kmp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.BaseMviStateManager
import com.adamczewski.kmpmvi.mvi.Closeable
import com.adamczewski.kmpmvi.mvi.progress.CombinedProgressPublisher
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.progress.ProgressManager
import com.adamczewski.kmpmvi.mvi.progress.ProgressObservable
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.utils.defaultSettings
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.error.BaseErrorManager
import com.adamczewski.kmpmvi.mvi.error.MviError
import com.adamczewski.kmpmvi.mvi.error.observeError
import com.adamczewski.kmpmvi.mvi.lifecycle.MviLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

public typealias MviViewModel<A, S, E> = BaseMviViewModel<A, S, E, Nothing>

public abstract class BaseMviViewModel<Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage>(
    initialState: State,
    settings: MviSettings? = null,
    vararg closeables: Closeable = arrayOf(),
) : ViewModel(), MviComponent<Action, State, Effect> {

    private val closeables = mutableListOf(*closeables)

    protected val container: BaseMviContainer<Action, State, Effect, Message> =
        BaseMviContainer<Action, State, Effect, Message>(
            scopeProvider = { viewModelScope },
            initialState = initialState,
            settings = settings ?: settings()
        )

    protected val scope: CoroutineScope = container.scope

    override val state: StateFlow<State> = container.state

    override val observableState: StateFlow<State> = container.observableState

    protected val stateValue: State
        get() = state.value

    override val effects: EffectsHandler<Effect> = container.effects

    public val progress: ProgressManager = container.progress

    public val messages: Flow<Message> = container.messenger.messages

    public val lifecycle: StateFlow<MviLifecycle> = container.lifecycle

    init {
        container.handleActions {
            handleActions()
        }
    }

    /**
     * Override this method to handle actions.
     * It's recommended to call only methods from provided [ActionsManager] in this method.
     * Other methods should be called within actions handling functions.
     */
    protected abstract fun ActionsManager<Action>.handleActions()

    open protected fun settings(): MviSettings {
        return defaultSettings()
    }

    public fun onInit(block: suspend () -> Unit) {
        container.onInit(block)
    }

    public fun onSubscribe(block: suspend () -> Unit) {
        container.onSubscribe(block)
    }

    public fun onUnsubscribe(block: suspend () -> Unit) {
        container.onUnsubscribe(block)
    }

    override fun onCleared() {
        container.clear()
        closeables.forEach { it.close() }
        super.onCleared()
    }

    final override fun submitAction(action: Action) {
        container.submitAction(action)
    }

    protected fun setState(reducer: State.() -> State) {
        container.setState(reducer)
    }

    protected suspend fun setEffect(
        reducer: suspend State.() -> Effect,
    ) {
        container.setEffect(requireConsumer = false, reducer)
    }

    protected suspend fun setEffectIfActive(
        reducer: suspend State.() -> Effect,
    ) {
        container.setEffect(requireConsumer = true, reducer)
    }

    protected suspend fun setMessage(reducer: suspend State.() -> Message) {
        container.messenger.setMessage(reducer(stateValue))
    }

    protected inline fun <reified M : MviMessage> onMessageFlow(
        child: BaseMviStateManager<*, *, *, in M>,
        noinline flowMapper: suspend Flow<M>.() -> Flow<*>,
    ) {
        container.messenger.onMessageFlow(M::class, child, flowMapper)
    }

    protected inline fun <reified M : MviMessage> onMessage(
        child: BaseMviStateManager<*, *, *, in M>,
        noinline block: suspend (M) -> Unit,
    ) {
        container.messenger.onMessage(M::class, child, block)
    }

    protected fun observeProgress(
        vararg progressObservables: ProgressObservable,
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit,
    ) {
        container.observeProgress(CombinedProgressPublisher(*progressObservables), block)
    }

    protected fun observeProgress(
        progressObservable: ProgressObservable,
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit,
    ) {
        container.observeProgress(progressObservable, block)
    }

    protected fun observeProgress(
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit,
    ) {
        container.observeProgress(block)
    }

    protected suspend fun <T> withProgress(block: suspend () -> T): T =
        container.withProgress(block)

    protected fun <E : MviError> observeError(
        errorManager: BaseErrorManager<E>,
        block: suspend CoroutineScope.(E?) -> Unit,
    ) {
        container.observeError(errorManager, block)
    }

    protected fun addCloseable(closeable: Closeable) {
        closeables.add(closeable)
    }
}
