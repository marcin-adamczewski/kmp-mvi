package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.error.ErrorManager
import com.adamczewski.kmpmvi.mvi.error.UiError
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.error.observeError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

abstract class MviStateManager<Action : MviAction, State : MVIState, Effect : MviEffect>(
    initialState: State,
    settings: MviComponent.Settings? = null,
    vararg closeables: Closeable = arrayOf(),
) : Closeable, StateComponent<Action, State, Effect> {
    private val closeables = mutableListOf(*closeables)

    protected val component = MviComponent<Action, State, Effect>(
        { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) },
        initialState,
        settings ?: defaultSettings()
    )

    protected val scope = component.scope

    init {
        component.actions.handleActions()
    }

    /**
     * Override this method to handle actions.
     * It's recommended to call only methods from provided [ActionsManager] in this method.
     * Other methods should be called within actions handling functions.
     */
    protected abstract fun ActionsManager<Action>.handleActions()

    override fun close() {
        closeables.forEach { it.close() }
        component.clear()
    }

    override val currentState = component.currentState

    protected val stateValue: State
        get() = currentState.value

    override val effects = component.effects

    val progress = component.progress

    final override fun submitAction(action: Action) {
        component.submitAction(action)
    }

    protected suspend fun setState(reducer: State.() -> State) {
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
