package com.adamczewski.kmpmvi.mvi.actions

import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.utils.MarkedFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
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

    /**
     * Main function for handling actions. You should always use this method directly or via
     * other actions handling functions.
     */
    public inline fun <reified T : Action> onActionFlow(
        crossinline transformer: suspend Flow<T>.() -> Flow<*>,
    ) {
        val isCollected = CompletableDeferred(Unit)
        collectedActions.add(isCollected)

        scope.launch {
            val actionFlow = MarkedFlow(
                actions.filterIsInstance<T>(),
                onCollect = {
                    isCollected.complete(Unit)
                }
            )
            actionFlow
                .transformer()
                .onEach {
                    if (!actionFlow.collected) {
                        throw ActionNotSubscribedException(T::class)
                    }
                }
                .launchIn(scope)
        }
    }

    public inline fun <reified T : Action> onAction(
        crossinline block: suspend (T) -> Unit,
    ) {
        onActionFlow { flatMapMerge { flow { emit(block(it)) } } }
    }

    public inline fun <reified T : Action> onActionSingle(
        crossinline block: suspend (T) -> Unit,
    ) {
        onActionFlow { take(1).map(block) }
    }

    public inline fun <reified T : Action> onActionFlowSingle(
        crossinline flow: suspend (T) -> Flow<*>,
    ) {
        onActionFlow<T> { take(1).flatMapLatest { flow(it) } }
    }

    @PublishedApi
    internal class ActionNotSubscribedException(actionClass: KClass<out MviAction>) : Throwable(
        message = "Action $actionClass flow was not collected. " +
                "Make sure to collect it in " +
                "the Flow receiver passed to onActionFlow"
    )
}
