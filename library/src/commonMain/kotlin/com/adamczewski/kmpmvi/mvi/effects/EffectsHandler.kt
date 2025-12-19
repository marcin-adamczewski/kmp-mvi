package com.adamczewski.kmpmvi.mvi.effects

import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.utils.AtomicMutableSet
import kotlin.reflect.KClass
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.decrementAndFetch
import kotlin.concurrent.atomics.incrementAndFetch

class EffectsHandler<T : MviEffect>(
    @PublishedApi internal val unconsumedEffectsFlow: Flow<UniqueEffect<T>>,
    @PublishedApi internal val consume: suspend (UniqueEffect<T>) -> Unit,
) {
    private var activeConsumers = AtomicInt(0)

    @PublishedApi
    internal var activeSingleEffectConsumers: Set<KClass<out T>> =
        AtomicMutableSet<KClass<out T>>()
    val observeEffects: Flow<T> = unconsumedEffectsFlow.map { it.effect }

    suspend fun consume(
        skipConsuming: (T) -> Boolean = { false },
        handler: suspend (T) -> Unit,
    ) {
        consumeFlow(
            skipConsuming = skipConsuming,
            handler = handler
        ).collect {}
    }

    fun consumeFlow(
        skipConsuming: (T) -> Boolean = { false },
        handler: suspend (T) -> Unit,
    ): Flow<T> {
        return unconsumedEffectsFlow
            .onStart { activeConsumers.incrementAndFetch() }
            .onCompletion { activeConsumers.decrementAndFetch() }
            .map { uniqueEffect ->
                handleAndConsumeEffect(
                    uniqueEffect,
                    uniqueEffect.effect,
                    handler,
                    shouldConsume = !skipConsuming(uniqueEffect.effect)
                )
            }
    }

    inline fun <reified B : T> consumeEffectFlow(
        noinline handler: suspend (B) -> Unit,
    ): Flow<B> {
        return unconsumedEffectsFlow
            .onStart { activeSingleEffectConsumers += B::class }
            .onCompletion { activeSingleEffectConsumers -= B::class }
            .filter { it.effect is B }
            .map { uniqueEffect ->
                val effect = uniqueEffect.effect as B
                handleAndConsumeEffect(uniqueEffect, effect, handler)
            }
    }

    /**
     * Consumes effect of type B. It's useful for consuming effects that inherit from
     * a common base class that is not the Effect class. Note that you can't detect active
     * consumers for this effect type using this function, so using it with setEffectIfActive
     * is not recommended.
     */
    inline fun <reified B> consumeBaseEffectFlow(
        noinline handler: suspend (B) -> Unit,
    ): Flow<B> {
        return unconsumedEffectsFlow
            .filter { it.effect is B }
            .map { uniqueEffect ->
                val effect = uniqueEffect.effect as B
                handleAndConsumeEffect(uniqueEffect, effect, handler)
            }
    }

    @PublishedApi
    internal suspend fun <B> handleAndConsumeEffect(
        uniqueEffect: UniqueEffect<T>,
        effect: B,
        handler: suspend (B) -> Unit,
        shouldConsume: Boolean = true,
    ): B {
        handler(effect)
        withContext(NonCancellable) {
            if (shouldConsume) {
                consume(uniqueEffect)
            }
        }

        return effect
    }

    internal fun isEffectConsumerActive(effect: MviEffect): Boolean {
        return activeConsumers.load() > 0 || activeSingleEffectConsumers.contains(effect::class)
    }
}
