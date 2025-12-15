package kmpmvi

import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.MviEffect
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.effects.UniqueEffect
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.spyk
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EffectsHandlerTest {

    private val effectsFlow = MutableSharedFlow<UniqueEffect<TestEffect>>()
    private val consumeEffect: (UniqueEffect<TestEffect>) -> Unit = spyk()

    private fun createSut(): EffectsHandler<TestEffect> {
        return EffectsHandler(effectsFlow, consumeEffect)
    }

    @Test
    fun `given consumeFlow called, when two effects emitted, then consume two effects`() = runTest {
        val sut = createSut()
        coVerify(exactly = 0) { consumeEffect.invoke(any()) }
        sut.consumeFlow(handler = {})
            .test {
                effectsFlow.emit(UniqueEffect(TestEffect.Navigate("first")))
                effectsFlow.emit(UniqueEffect(TestEffect.Refresh))

                coVerifySequence {
                    consumeEffect.invoke(match { it.effect == TestEffect.Navigate("first") })
                    consumeEffect.invoke(match { it.effect == TestEffect.Refresh })
                }
                cancelAndConsumeRemainingEvents()
            }
    }

    @Test
    fun `given consumeFlow called, when two effects emitted but the first should be not consumed, then consume only the second effect`() = runTest {
        val sut = createSut()
        coVerify(exactly = 0) { consumeEffect.invoke(any()) }
        sut.consumeFlow(handler = {}, skipConsuming = { it is TestEffect.Navigate })
            .test {
                effectsFlow.emit(UniqueEffect(TestEffect.Navigate("first")))
                effectsFlow.emit(UniqueEffect(TestEffect.Refresh))

                coVerifySequence {
                    consumeEffect.invoke(match { it.effect == TestEffect.Refresh })
                }
                cancelAndConsumeRemainingEvents()
            }
    }

    @Test
    fun `when consume specific effects, then only consume those effects`() = runTest {
        val sut = createSut()
        coVerify(exactly = 0) { consumeEffect.invoke(any()) }
        sut.consumeEffectFlow<TestEffect.Navigate>(handler = {})
            .test {
                effectsFlow.emit(UniqueEffect(TestEffect.Navigate("first")))
                effectsFlow.emit(UniqueEffect(TestEffect.Refresh))
                effectsFlow.emit(UniqueEffect(TestEffect.Navigate("second")))

                coVerifySequence {
                    consumeEffect.invoke(match { it.effect == TestEffect.Navigate("first") })
                    consumeEffect.invoke(match { it.effect == TestEffect.Navigate("second") })
                }
                cancelAndConsumeRemainingEvents()
            }
    }

    @Test
    fun `when consume base effects, then only consume those effects`() = runTest {
        val sut = createSut()
        coVerify(exactly = 0) { consumeEffect.invoke(any()) }
        sut.consumeBaseEffectFlow<BaseEffect>(handler = {})
            .test {
                effectsFlow.emit(UniqueEffect(TestEffect.Refresh))
                effectsFlow.emit(UniqueEffect(TestEffect.ChildBaseEffect))

                coVerifySequence {
                    consumeEffect.invoke(match { it.effect == TestEffect.ChildBaseEffect })
                }
                cancelAndConsumeRemainingEvents()
            }
    }

    @Test
    fun `given consumeFlow called, when handling effect interrupted with CancellationException, then do not consume effect`() =
        runTest {
            val sut = createSut()
            sut.consumeFlow(handler = { throw CancellationException() })
                .test {
                    effectsFlow.emit(UniqueEffect(TestEffect.Refresh))
                    coVerify(exactly = 0) { consumeEffect.invoke(any()) }
                    cancelAndConsumeRemainingEvents()
                }
        }

    @Test
    fun `given consumeFlow called, when handling effect interrupted with non CancellationException, then do not consume effect`() =
        runTest {
            val sut = createSut()
            sut.consumeFlow(handler = { throw IllegalStateException() })
                .test {
                    effectsFlow.emit(UniqueEffect(TestEffect.Refresh))
                    coVerify(exactly = 0) { consumeEffect.invoke(any()) }
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

private sealed interface TestEffect : MviEffect {
    data class Navigate(val route: String) : TestEffect
    data object Refresh : TestEffect
    data object ChildBaseEffect : TestEffect, BaseEffect
}