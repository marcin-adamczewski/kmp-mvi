package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.effects.EffectsManager
import com.adamczewski.kmpmvi.mvi.logger.MviLogger
import com.adamczewski.kmpmvi.mvi.logger.NoOpLogger
import com.adamczewski.kmpmvi.mvi.messenger.Messenger
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.model.NoMessages
import com.adamczewski.kmpmvi.mvi.progress.ProgressCounter
import com.adamczewski.kmpmvi.mvi.progress.ProgressObservable
import com.adamczewski.kmpmvi.mvi.progress.withProgress
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.PublishedApi
import kotlin.coroutines.EmptyCoroutineContext

typealias MviComponent<A, S, E> = BaseMviComponent<A, S, E, NoMessages>

class BaseMviComponent<Action : MviAction, State: MviState, Effects : MviEffect, Message: MviMessage>(
    scopeProvider: () -> CoroutineScope,
    initialState: State,
    @PublishedApi internal val settings: MviSettings,
) : StateComponent<Action, State, Effects> {
    private val handleActionCalled = CompletableDeferred<Unit>()

    private val logger: MviLogger by lazy {
        if (MviConfig.canLog) {
            settings.logger()
        } else {
            NoOpLogger()
        }
    }

    @PublishedApi
    internal val scope: CoroutineScope = CoroutineScope(
        scopeProvider().coroutineContext.let { context ->
            val exceptionHandler =
                context[CoroutineExceptionHandler.Key] ?: settings.exceptionHandler
                ?: EmptyCoroutineContext
            context + exceptionHandler
        }
    )

    private val stateFlow = MutableStateFlow(initialState)

    private val effectsManager = EffectsManager<Effects>(settings.effectsBufferSize)

    private val isStateSubscribed: StateFlow<Boolean?> = stateFlow.subscriptionCount
        .drop(2) // We're not interested in initial 0 value and the first logger subscriber
        .map { it >= MIN_SUBSCRIBERS_COUNT }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    private val actions = ActionsManager<Action>(scope, handleActionCalled)

    val progress = ProgressCounter()

    override val currentState: StateFlow<State> = stateFlow

    override val effects: EffectsHandler<Effects> = effectsManager.effectsHandler

    val subscribersCount: StateFlow<Int> = stateFlow.subscriptionCount

    val messenger = Messenger<Message>(scope)

    init {
        scope.launch {
            initLogger(initialState)
        }
    }

    fun handleActions(block: ActionsManager<Action>.() -> Unit) {
        actions.block()
        handleActionCalled.complete(Unit)
    }

    fun onInit(block: suspend () -> Unit) {
        isStateSubscribed
            .filter { it == true }
            .take(1)
            .onEach {
                logger.onInit()
                block()
            }
            .launchIn(scope)
    }


    fun onSubscribe(block: suspend () -> Unit) {
        isStateSubscribed
            .filter { it == true }
            .onEach {
                logger.onSubscribe()
                block()
            }
            .launchIn(scope)
    }

    fun onUnsubscribe(block: suspend () -> Unit) {
        isStateSubscribed
            .filter { it == false }
            .onEach {
                logger.onUnsubscribe()
                block()
            }
            .launchIn(scope)
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
        logger.onClear()
        scope.cancel()
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

        scope.launch {
            messenger.messages.collect {
                logger.onMessage(it)
            }
        }
    }

    private companion object {
        // One subscriber is for a logger
        private const val MIN_SUBSCRIBERS_COUNT = 2
    }
}
