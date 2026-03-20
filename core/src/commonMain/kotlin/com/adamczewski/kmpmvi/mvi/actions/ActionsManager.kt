package com.adamczewski.kmpmvi.mvi.actions

import com.adamczewski.kmpmvi.mvi.model.MviAction
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

public class ActionsManager<Action : MviAction>(
    @PublishedApi internal val scope: CoroutineScope,
    private val actionsLock: CompletableDeferred<Unit>,
) {
    @PublishedApi
    internal val collectedActions: MutableList<CompletableDeferred<Unit>> =
        mutableListOf<CompletableDeferred<Unit>>()
    @PublishedApi
    internal val actions: MutableSharedFlow<Action> =
        MutableSharedFlow<Action>(extraBufferCapacity = 5)

    public fun submitAction(action: Action) {
        scope.launch {
            actionsLock.await()
            collectedActions.awaitAll()
            actions.emit(action)
        }
    }

    public inline fun <reified T : Action> onActionFlow(
        noinline transformer: suspend Flow<T>.() -> Flow<*>,
    ) {
        onActionFlowInternal(T::class, transformer)
    }

    public inline fun <reified T : Action> onAction(
        noinline block: suspend (T) -> Unit,
    ) {
        onActionFlowInternal(T::class) {
            flatMapMerge { flow { emit(block(it)) } }
        }
    }

    public inline fun <reified T : Action> onActionSingle(
        noinline block: suspend (T) -> Unit,
    ) {
        onActionFlowInternal(T::class) {
            take(1).map(block)
        }
    }

    public inline fun <reified T : Action> onActionFlowSingle(
        noinline flow: suspend (T) -> Flow<*>,
    ) {
        onActionFlowInternal(T::class) {
            take(1).flatMapLatest { flow(it) }
        }
    }

    /**
     * Main function for handling actions. You should always use this method directly or via
     * other actions handling functions.
     */
    @PublishedApi
    internal fun <T : Action> onActionFlowInternal(
        actionClass: KClass<T>,
        transformer: suspend Flow<T>.() -> Flow<*>,
    ) {
        val isReadyToReceiveActions = CompletableDeferred<Unit>()
        collectedActions.add(isReadyToReceiveActions) // submitAction waits for this

        scope.launch {
            val actionChannel = Channel<T>(Channel.BUFFERED)
            launch {
                actions
                    .filterIsInstance(actionClass)
                    .onStart {
                        // Signal that we are now buffering actions (unblocks submitAction)
                        isReadyToReceiveActions.complete(Unit)
                    }
                    .collect { actionChannel.send(it) }
            }

            // Wait for the first action. This suspension point
            // allows the MVI subclasses initialization to finish safely.
            val firstAction = actionChannel.receive()

            // This ensures that transformer was called on the receiver flow of onAction method.
            val isActionsSourceCollected = CompletableDeferred<Unit>()

            val actionsSource = flow {
                emit(firstAction)
                emitAll(actionChannel.receiveAsFlow())
            }.onStart { isActionsSourceCollected.complete(Unit) }

            actionsSource.transformer().collect {
                if (!isActionsSourceCollected.isCompleted) {
                    throw ActionNotSubscribedException(actionClass)
                }
            }
        }
    }
}

internal class ActionNotSubscribedException(actionClass: KClass<out MviAction>) : Throwable(
    message = "Action $actionClass flow was not collected. " +
            "Make sure to pass Flow transformer instead of a plain Flow."
)
