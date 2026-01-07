package kmpmvi

import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.effects.UniqueEffect
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


class EffectsHandlerTest {

    private val effectsFlow = MutableSharedFlow<UniqueEffect<TestEffect>>()

    private fun createSut(
        consumer: EffectConsumer = EffectConsumer()
    ): EffectsHandler<TestEffect> {
        return EffectsHandler(effectsFlow, consumer::consume)
    }

    @Test
    fun `given consumeFlow called, when two effects emitted, then consume two effects`() = runTest {
        val consumer = EffectConsumer()
        val sut = createSut(consumer)
        assertEquals(0, consumer.consumedEffectsCount())
        sut.consumeFlow(handler = {})
            .test {
                effectsFlow.emit(UniqueEffect(TestEffect.Navigate("first")))
                effectsFlow.emit(UniqueEffect(TestEffect.Refresh))

                assertEquals(2, consumer.consumedEffects.size)
                assertEquals(consumer.consumedEffects.get(0).effect, TestEffect.Navigate("first"))
                assertEquals(consumer.consumedEffects.get(1).effect, TestEffect.Refresh)
                cancelAndConsumeRemainingEvents()
            }
    }

    @Test
    fun `given consumeFlow called, when two effects emitted but the first should be not consumed, then consume only the second effect`() =
        runTest {
            val consumer = EffectConsumer()
            val sut = createSut(consumer)
            assertEquals(0, consumer.consumedEffectsCount())
            sut.consumeFlow(handler = {}, skipConsuming = { it is TestEffect.Navigate })
                .test {
                    effectsFlow.emit(UniqueEffect(TestEffect.Navigate("first")))
                    effectsFlow.emit(UniqueEffect(TestEffect.Refresh))

                    assertEquals(1, consumer.consumedEffects.size)
                    assertEquals(consumer.consumedEffects.get(0).effect, TestEffect.Refresh)
                    cancelAndConsumeRemainingEvents()
                }
        }

    @Test
    fun `when consume specific effects, then only consume those effects`() = runTest {
        val consumer = EffectConsumer()
        val sut = createSut(consumer)
        assertEquals(0, consumer.consumedEffectsCount())
        sut.consumeEffectFlow<TestEffect.Navigate>(handler = {})
            .test {
                effectsFlow.emit(UniqueEffect(TestEffect.Navigate("first")))
                effectsFlow.emit(UniqueEffect(TestEffect.Refresh))
                effectsFlow.emit(UniqueEffect(TestEffect.Navigate("second")))

                assertEquals(2, consumer.consumedEffects.size)
                assertEquals(consumer.consumedEffects.get(0).effect, TestEffect.Navigate("first"))
                assertEquals(consumer.consumedEffects.get(1).effect, TestEffect.Navigate("second"))
                cancelAndConsumeRemainingEvents()
            }
    }

    @Test
    fun `when consume base effects, then only consume those effects`() = runTest {
        val consumer = EffectConsumer()
        val sut = createSut(consumer)
        assertEquals(0, consumer.consumedEffectsCount())
        sut.consumeBaseEffectFlow<BaseEffect>(handler = {})
            .test {
                effectsFlow.emit(UniqueEffect(TestEffect.Refresh))
                effectsFlow.emit(UniqueEffect(TestEffect.ChildBaseEffect))

                assertEquals(1, consumer.consumedEffects.size)
                assertEquals(consumer.consumedEffects.get(0).effect, TestEffect.ChildBaseEffect)
                cancelAndConsumeRemainingEvents()
            }
    }

    @Test
    fun `given consumeFlow called, when handling effect interrupted with CancellationException, then do not consume effect`() =
        runTest {
            val consumer = EffectConsumer()
            val sut = createSut(consumer)
            sut.consumeFlow(handler = { throw CancellationException() })
                .test {
                    effectsFlow.emit(UniqueEffect(TestEffect.Refresh))
                    assertEquals(0, consumer.consumedEffectsCount())
                    cancelAndConsumeRemainingEvents()
                }
        }

    @Test
    fun `given consumeFlow called, when handling effect interrupted with non CancellationException, then do not consume effect`() =
        runTest {
            val consumer = EffectConsumer()
            val sut = createSut(consumer)
            sut.consumeFlow(handler = { throw IllegalStateException() })
                .test {
                    effectsFlow.emit(UniqueEffect(TestEffect.Refresh))
                    assertEquals(0, consumer.consumedEffectsCount())
                    cancelAndConsumeRemainingEvents()
                }
        }

    @Nested
    inner class ActiveConsumers {
        @Test
        fun `when consumeFlow called, then effects have active consumer while subscription is active`() =
            runTest {
                val sut = createSut()
                assertFalse(sut.isEffectConsumerActive(TestEffect.Refresh))
                assertFalse(sut.isEffectConsumerActive(TestEffect.ChildBaseEffect))

                sut.consumeFlow(handler = { })
                    .test {
                        assertTrue(sut.isEffectConsumerActive(TestEffect.Refresh))
                        assertTrue(sut.isEffectConsumerActive(TestEffect.ChildBaseEffect))

                        cancelAndConsumeRemainingEvents()

                        assertFalse(sut.isEffectConsumerActive(TestEffect.Refresh))
                        assertFalse(sut.isEffectConsumerActive(TestEffect.ChildBaseEffect))
                    }
            }

        @Test
        fun `when consumeEffectFlow called, then only given effect has active consumer while subscription is active`() =
            runTest {
                val sut = createSut()
                assertFalse(sut.isEffectConsumerActive(TestEffect.Refresh))
                assertFalse(sut.isEffectConsumerActive(TestEffect.ChildBaseEffect))

                sut.consumeEffectFlow<TestEffect.ChildBaseEffect> {}
                    .test {
                        assertFalse(sut.isEffectConsumerActive(TestEffect.Refresh))
                        assertTrue(sut.isEffectConsumerActive(TestEffect.ChildBaseEffect))

                        cancelAndConsumeRemainingEvents()

                        assertFalse(sut.isEffectConsumerActive(TestEffect.Refresh))
                        assertFalse(sut.isEffectConsumerActive(TestEffect.ChildBaseEffect))
                    }
            }

        @Test
        fun `when consume active, then effects have active consumer while subscription is active`() =
            runTest {
                val sut = createSut()
                assertFalse(sut.isEffectConsumerActive(TestEffect.Refresh))
                assertFalse(sut.isEffectConsumerActive(TestEffect.ChildBaseEffect))

                val job = launch {
                    sut.consume(handler = { })
                }
                advanceUntilIdle()

                assertTrue(sut.isEffectConsumerActive(TestEffect.Refresh))
                assertTrue(sut.isEffectConsumerActive(TestEffect.ChildBaseEffect))

                job.cancel()
                advanceUntilIdle()

                assertFalse(sut.isEffectConsumerActive(TestEffect.Refresh))
                assertFalse(sut.isEffectConsumerActive(TestEffect.ChildBaseEffect))
            }
    }
}

private interface BaseEffect

internal sealed interface TestEffect : MviEffect {
    data class Navigate(val route: String) : TestEffect
    data object Refresh : TestEffect
    data object ChildBaseEffect : TestEffect, BaseEffect
}

private class EffectConsumer {
    private var _consumedEffects: MutableList<UniqueEffect<TestEffect>> = mutableListOf()
    internal val consumedEffects: List<UniqueEffect<TestEffect>> = _consumedEffects

    internal suspend fun consume(effect: UniqueEffect<TestEffect>) {
        _consumedEffects.add(effect)
    }

    internal fun consumedEffectsCount() = _consumedEffects.size
}
