package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.effects.EffectsManager
import com.adamczewski.kmpmvi.mvi.lifecycle.LifecycleManager
import com.adamczewski.kmpmvi.mvi.logger.MviLogger
import com.adamczewski.kmpmvi.mvi.logger.NoOpLogger
import com.adamczewski.kmpmvi.mvi.messenger.Messenger
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.progress.ProgressManager
import com.adamczewski.kmpmvi.mvi.progress.ProgressObservable
import com.adamczewski.kmpmvi.mvi.progress.withProgress
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import com.adamczewski.kmpmvi.mvi.state.StateManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

public typealias MviContainer<A, S, E> = BaseMviContainer<A, S, E, Nothing>

public class BaseMviContainer<Action : MviAction, State: MviState, Effects : MviEffect, Message: MviMessage>(
    scopeProvider: () -> CoroutineScope,
    initialState: State,
    @PublishedApi internal val settings: MviSettings,
) : MviComponent<Action, State, Effects> {
    private val handleActionCalled = CompletableDeferred<Unit>()

    private val logger: MviLogger by lazy {
        if (settings.isLoggerEnabled) {
            settings.logger()
        } else {
            NoOpLogger()
        }
    }

    public val scope: CoroutineScope = CoroutineScope(
        scopeProvider().coroutineContext.let { context ->
            val exceptionHandler =
                context[CoroutineExceptionHandler.Key] ?: settings.exceptionHandler
                ?: EmptyCoroutineContext
            context + exceptionHandler
        }
    )

    private val stateManager: StateManager<State> = StateManager(initialState)
    override val state: StateFlow<State> = stateManager.subscriberCountState
    override val observableState: StateFlow<State> = stateManager.state
    public val subscribersCount: StateFlow<Int> = stateManager.subscribersCount

    private val effectsManager = EffectsManager<Effects>(settings.effectsBufferSize)
    override val effects: EffectsHandler<Effects> = effectsManager.effectsHandler

    private val actions = ActionsManager<Action>(scope, handleActionCalled)

    private val lifecycleManager = LifecycleManager(subscribersCount, scope, logger)

    public val progress: ProgressManager = ProgressManager()

    public val messenger: Messenger<Message> = Messenger<Message>(scope)

    init {
        scope.launch {
            initLogger(initialState)
        }
    }

    public  fun handleActions(block: ActionsManager<Action>.() -> Unit) {
        actions.block()
        handleActionCalled.complete(Unit)
    }

    public fun onInit(block: suspend () -> Unit) {
        lifecycleManager.onInit(block)
    }

    public fun onSubscribe(block: suspend () -> Unit) {
        lifecycleManager.onSubscribe(block)
    }

    public fun onUnsubscribe(block: suspend () -> Unit) {
        lifecycleManager.onUnsubscribe(block)
    }

    public fun setState(reducer: State.() -> State) {
        stateManager.setState(reducer)
    }

    public suspend fun setEffect(
        requireConsumer: Boolean = false,
        reducer: suspend State.() -> Effects,
    ) {
        effectsManager.setEffect(
            requireConsumer = requireConsumer,
            effect = reducer(stateManager.stateValue)
        )
    }

    override fun submitAction(action: Action) {
        actions.submitAction(action)
    }

    public fun observeProgress(
        progressObservable: ProgressObservable,
        block: suspend CoroutineScope.(showProgress: Boolean) -> Unit,
    ) {
        scope.launch {
            progressObservable.isLoading.collect { showProgress ->
                block(showProgress)
            }
        }
    }

    public fun observeProgress(block: suspend CoroutineScope.(showProgress: Boolean) -> Unit): Unit =
        observeProgress(progress, block)

    public suspend fun <T> withProgress(
        block: suspend () -> T,
    ): T {
        return withProgress(manager = progress, block = block)
    }

    public fun clear() {
        logger.onClear()
        scope.cancel()
    }

    private fun initLogger(initialState: State) {
        logger.onInitialState(initialState)

        scope.launch {
            observableState
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
}
