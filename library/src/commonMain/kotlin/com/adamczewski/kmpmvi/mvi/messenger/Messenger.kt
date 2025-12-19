package com.adamczewski.kmpmvi.mvi.messenger

import com.adamczewski.kmpmvi.mvi.BaseMviStateManager
import com.adamczewski.kmpmvi.mvi.MviMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class Messenger<M: MviMessage>(
    private val scope: CoroutineScope,
) {
    private val messagesFlow = MutableSharedFlow<M>(extraBufferCapacity = 10)
    val messages: SharedFlow<M> = messagesFlow.asSharedFlow()

    fun setMessage(message: M) {
        scope.launch {
            messagesFlow.emit(message)
        }
    }

    fun <M: MviMessage> onMessageFlow(
        klass: KClass<M>,
        child: BaseMviStateManager<*, *, *, in M>,
        transformer: suspend Flow<M>.() -> Flow<*>,
    ) {
        scope.launch {
            child.messages
                .filterIsInstance(klass)
                .transformer()
                .collect()
        }
    }

    fun <M: MviMessage> onMessage(
        klass: KClass<M>,
        child: BaseMviStateManager<*, *, *, in M>,
        block: suspend (M) -> Unit,
    ) {
        onMessageFlow(klass, child) { flatMapMerge { flow { emit(block(it)) } } }
    }
}
