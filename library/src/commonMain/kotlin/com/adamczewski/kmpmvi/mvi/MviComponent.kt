package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.effects.EffectsManager
import com.adamczewski.kmpmvi.mvi.logger.Logger
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface MviAction
object NoActions : MviAction

interface MVIState {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

data object NoState : MVIState

interface MviEffect
object NoEffects : MviEffect

class MviComponent<Action : MviAction, State: MVIState, Effects : MviEffect>(
    scopeProvider: () -> CoroutineScope,
    initialState: State,
    @PublishedApi internal val settings: Settings,
) : StateComponent<Action, State, Effects> {

    private val logger by lazy { settings.logger() }

    @PublishedApi
    internal val scope = CoroutineScope(
        scopeProvider().coroutineContext.let { context ->
            val exceptionHandler =
                context[CoroutineExceptionHandler.Key] ?: settings.exceptionHandler
                ?: EmptyCoroutineContext
            context + exceptionHandler
        }
    )

    private val stateFlow = MutableStateFlow(initialState)

    private val effectsManager = EffectsManager<Effects>(settings.effectsBufferSize)

    internal val actions = ActionsManager<Action>(scope)

    val progress = ProgressCounter()

    override val currentState: StateFlow<State> = stateFlow

    override val effects: EffectsHandler<Effects> = effectsManager.effectsHandler

    init {
        scope.launch {
            initLogger(initialState)
        }
    }

    fun setState(reducer: State.() -> State) {
        stateFlow.update { currentValue -> reducer(currentValue) }
    }

    suspend fun setEffect(
        requireConsumer: Boolean = false,
        reducer: suspend State.() -> Effects,
    ) {
        effectsManager.setEffect(
            requireConsumer = requireConsumer,
            effect = reducer(stateFlow.value)
        )
    }

    override fun submitAction(action: Action) {
        actions.submitAction(action)
    }

    fun observeProgress(
        progressObservable: ProgressObservable,
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit,
    ) {
        scope.launch {
            progressObservable.observeState.collect { showProgress ->
                block(showProgress)
            }
        }
    }

    fun observeProgress(block: suspend CoroutineScope.(showProgress: Boolean) -> Unit) =
        observeProgress(progress, block)

    suspend fun <T> withProgress(
        block: suspend () -> T,
    ): T {
        return withProgress(progress, block)
    }

    fun clear() {
        scope.cancel()
        logger.onCleared()
    }

    private fun initLogger(initialState: State) {
        logger.onInitialState(initialState)

        scope.launch {
            stateFlow
                .filter { it !== initialState }
                .collect {
                    logger.onState(it)
                }
        }

        scope.launch {
            effects.observeEffects
                .collect {
                    logger.onEffect(it)
                }
        }

        scope.launch {
            actions.actions.collect {
                logger.onAction(it)
            }
        }
    }

    class Settings(
        val logger: () -> Logger = { object : Logger {} },
        val effectsBufferSize: Int = 10,
        val exceptionHandler: CoroutineExceptionHandler? = null,
    )
}
