@file:OptIn(ExperimentalUuidApi::class)

package com.adamczewski.kmpmvi.mvi.effects

import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.utils.AtomicMutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class EffectsManager<T : MviEffect>(bufferSize: Int) {
    private val consumedEffectIds = AtomicMutableSet<String>()
    private val effectsFlow = MutableSharedFlow<UniqueEffect<T>>(
        replay = bufferSize,
        extraBufferCapacity = bufferSize
    )
    private val unconsumedEffects: Flow<UniqueEffect<T>> = effectsFlow
        // We can't use distinctUntilChanged() as we want to filter out effects that are consumed
        // (e.g. in UI), not effects that were emitted.
        .filter { wrapper -> !consumedEffectIds.contains(wrapper.id) }

    val effectsHandler = EffectsHandler(
        unconsumedEffectsFlow = unconsumedEffects,
        consume = { effect -> consumeEffect(effect) }
    )

    suspend fun setEffect(effect: T, requireConsumer: Boolean = false) {
        if (!requireConsumer || effectsHandler.isEffectConsumerActive(effect)) {
            effectsFlow.emit(UniqueEffect(effect))
        }
    }

    private fun consumeEffect(effect: UniqueEffect<out MviEffect>) {
        consumedEffectIds.add(effect.id)
    }
}

class UniqueEffect<T : MviEffect>(
    val effect: T,
    val id: String = Uuid.random().toString(),
)
