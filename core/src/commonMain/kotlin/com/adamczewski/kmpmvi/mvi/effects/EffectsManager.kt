package com.adamczewski.kmpmvi.mvi.effects

import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.utils.AtomicMutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlin.uuid.ExperimentalUuidApi

public class EffectsManager<T : MviEffect>(
    bufferSize: Int,
    idsLruCacheSize: Int = EFFECT_IDS_LRU_CACHE_SIZE
) {
    // The effect-id caches must be at least as large as the replay buffer. Otherwise a consumed
    // effect's id could be evicted while the effect is still in the replay buffer, so it would
    // pass the "unconsumed" filter and be delivered (and handled) again on the next subscription.
    private val effectIdsCacheSize = maxOf(idsLruCacheSize, bufferSize)
    private val consumedEffectIds = AtomicMutableSet<String>(maxSize = effectIdsCacheSize)
    private val effectsFlow = MutableSharedFlow<UniqueEffect<T>>(
        replay = bufferSize,
        extraBufferCapacity = bufferSize
    )
    private val unconsumedEffects: Flow<UniqueEffect<T>> = effectsFlow
        // We can't use distinctUntilChanged() as we want to filter out effects that are consumed
        // (e.g. in UI), not effects that were emitted.
        .filter { wrapper -> !consumedEffectIds.contains(wrapper.id) }

    public val effectsHandler: EffectsHandler<T> = EffectsHandler(
        unconsumedEffectsFlow = unconsumedEffects,
        consume = { effect -> consumeEffect(effect) },
        handledIdsCacheSize = effectIdsCacheSize
    )

    public suspend fun setEffect(effect: T, requireConsumer: Boolean = false) {
        if (!requireConsumer || effectsHandler.isEffectConsumerActive(effect)) {
            effectsFlow.emit(UniqueEffect(effect))
        }
    }

    private fun consumeEffect(effect: UniqueEffect<out MviEffect>) {
        consumedEffectIds.add(effect.id)
    }

    private companion object {
        private const val EFFECT_IDS_LRU_CACHE_SIZE = 30
    }
}
