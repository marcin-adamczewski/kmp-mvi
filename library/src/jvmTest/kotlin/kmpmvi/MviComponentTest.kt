package kmpmvi

import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.MVIState
import com.adamczewski.kmpmvi.mvi.MviAction
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.MviEffect
import com.adamczewski.kmpmvi.mvi.Settings
import com.adamczewski.kmpmvi.mvi.logger.Logger
import com.adamczewski.kmpmvi.test.testEffects
import com.adamczewski.kmpmvi.test.testState
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class MviComponentTest {

    @AfterEach
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    private fun createSut(
        initialState: TestState = TestState(),
        effectsBufferSize: Int = 10,
        exceptionHandler: CoroutineExceptionHandler? = null,
    ): MviComponent<TestAction, TestState, TestEffect> {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        return MviComponent<TestAction, TestState, TestEffect>(
            scopeProvider = { CoroutineScope(SupervisorJob() + Dispatchers.Main) },
            initialState = initialState,
            settings = Settings(
                logger = { NoOpLogger() },
                effectsBufferSize = effectsBufferSize,
                exceptionHandler = exceptionHandler,
            )
        )
    }

    @Nested
    inner class Effects {
        @Test
        fun `when emitting effects before any subscription, then emit all effects when subscribed`() =
            runTest {
                val sut = createSut()
                sut.setEffect { TestEffect.Navigate("first") }
                sut.setEffect { TestEffect.Refresh }
                sut.setEffect { TestEffect.Refresh }

                sut.testEffects(this) {
                    assertEquals(TestEffect.Navigate("first"), awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        fun `when emitting effects after subscription, then emit all effects when subscribed`() =
            runTest {
                val sut = createSut()
                sut.testEffects(this) {
                    expectNoEvents()
                    sut.setEffect { TestEffect.Navigate("first") }
                    sut.setEffect { TestEffect.Refresh }
                    sut.setEffect { TestEffect.Refresh }

                    assertEquals(TestEffect.Navigate("first"), awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        fun `when emitted effects consumed, then effects not repeated between subscriptions`() =
            runTest(UnconfinedTestDispatcher()) {
                val sut = createSut()

                sut.setEffect { TestEffect.Navigate("first") }
                sut.setEffect { TestEffect.Refresh }

                sut.testEffects(this) {
                    assertEquals(TestEffect.Navigate("first"), awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    cancelAndIgnoreRemainingEvents()

                    sut.testEffects(this@runTest) {
                        expectNoEvents()
                        cancel()
                    }
                }
            }

        @Test
        fun `when observing effects without consuming, then effects repeated on the next subscription`() =
            runTest {
                val sut = createSut()
                val observedEffects = sut.effects.observeEffects
                sut.setEffect { TestEffect.Navigate("first") }
                sut.setEffect { TestEffect.Refresh }

                observedEffects.test {
                    assertEquals(TestEffect.Navigate("first"), awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    cancelAndIgnoreRemainingEvents()

                    observedEffects.test {
                        assertEquals(TestEffect.Navigate("first"), awaitItem())
                        assertEquals(TestEffect.Refresh, awaitItem())
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

        @Test
        fun `when observing and consuming effects in the same time, then effects multicasted to both subscribers`() =
            runTest {
                val sut = createSut()
                sut.effects.observeEffects.test {
                    val observeEffectsTest = this@test
                    sut.testEffects(this@runTest) {
                        val consumeEffectsTest = this@testEffects

                        sut.setEffect { TestEffect.Navigate("first") }
                        sut.setEffect { TestEffect.Refresh }

                        assertEquals(TestEffect.Navigate("first"), consumeEffectsTest.awaitItem())
                        assertEquals(TestEffect.Refresh, consumeEffectsTest.awaitItem())
                        assertEquals(TestEffect.Navigate("first"), observeEffectsTest.awaitItem())
                        assertEquals(TestEffect.Refresh, observeEffectsTest.awaitItem())
                    }
                }
            }

        @Test
        fun `when effect was already consumed, then new subscriber receives only new effects`() =
            runTest {
                val sut = createSut()

                sut.testEffects(this) {
                    sut.setEffect { TestEffect.Navigate("/first") }
                    assertEquals(TestEffect.Navigate("/first"), awaitItem())
                    cancelAndIgnoreRemainingEvents()

                    sut.testEffects(this@runTest) {
                        sut.setEffect { TestEffect.Navigate("/second") }
                        assertEquals(TestEffect.Navigate("/second"), awaitItem())
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

        @Test
        fun `when not all effects are consumed, then unconsumed effects are emitted on the next subscription`() =
            runTest {
                val sut = createSut()
                sut.setEffect { TestEffect.Navigate("first") }
                sut.setEffect { TestEffect.Refresh }
                sut.setEffect { TestEffect.Navigate("second") }
                sut.setEffect { TestEffect.Refresh }

                sut.effects.consumeFlow(
                    handler = {},
                    skipConsuming = { it is TestEffect.Refresh }
                )
                    .test {
                        assertEquals(TestEffect.Navigate("first"), awaitItem())
                        assertEquals(TestEffect.Refresh, awaitItem())
                        assertEquals(TestEffect.Navigate("second"), awaitItem())
                        assertEquals(TestEffect.Refresh, awaitItem())
                        expectNoEvents()
                    }

                sut.effects.consumeFlow(handler = {})
                    .test {
                        assertEquals(TestEffect.Refresh, awaitItem())
                        assertEquals(TestEffect.Refresh, awaitItem())
                        expectNoEvents()
                        cancel()
                    }
            }

        @Test
        fun `when same effects emitted twice in a row, then receive them twice`() =
            runTest {
                val sut = createSut()

                sut.testEffects(this) {
                    sut.setEffect { TestEffect.Navigate("/first") }
                    sut.setEffect { TestEffect.Navigate("/first") }
                    sut.setEffect { TestEffect.Refresh }
                    sut.setEffect { TestEffect.Refresh }

                    assertEquals(TestEffect.Navigate("/first"), awaitItem())
                    assertEquals(TestEffect.Navigate("/first"), awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        fun `when effects count is over buffer size, then emit only last effects that fits the buffer`() =
            runTest {
                val sut = createSut(effectsBufferSize = 3)

                sut.setEffect { TestEffect.Navigate("/first") }
                sut.setEffect { TestEffect.Navigate("/second") }
                sut.setEffect { TestEffect.Refresh }
                sut.setEffect { TestEffect.Refresh }

                sut.testEffects(this) {
                    assertEquals(TestEffect.Navigate("/second"), awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    assertEquals(TestEffect.Refresh, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        fun `given no active consumer, when emitting effects with required active consumer, then do not emit effects when consumer available`() =
            runTest {
                val sut = createSut()

                sut.setEffect(requireConsumer = true) { TestEffect.Navigate("first") }
                sut.setEffect(requireConsumer = true) { TestEffect.Refresh }

                sut.effects.consumeFlow { }
                    .test {
                        expectNoEvents()
                        cancel()
                    }
            }

        @Test
        fun `given no active consumer, when consumeEffectFlow called, then emit only effects that do not require active consumer`() =
            runTest {
                val sut = createSut()

                sut.setEffect(requireConsumer = true) { TestEffect.Navigate("first") }
                sut.setEffect(requireConsumer = false) { TestEffect.Refresh }

                sut.effects.consumeEffectFlow<TestEffect.Refresh> { }
                    .test {
                        assertEquals(TestEffect.Refresh, awaitItem())
                        expectNoEvents()
                        cancel()
                    }
            }

        @Test
        fun `given active consumer, when emitting effects with required active consumer, then effects emitted`() =
            runTest {
                val sut = createSut()
                sut.effects.consumeFlow { }
                    .test {
                        sut.setEffect(requireConsumer = true) { TestEffect.Navigate("first") }
                        sut.setEffect(requireConsumer = true) { TestEffect.Refresh }

                        assertEquals(TestEffect.Navigate("first"), awaitItem())
                        assertEquals(TestEffect.Refresh, awaitItem())
                        expectNoEvents()
                        cancel()
                    }
            }
    }

    @Nested
    inner class Actions {
        @Test
        fun `when flow passed to onActionFlow, then throw error`() {
            assertFailsWith<IllegalStateException> {
                runTest {
                    createSut().actions.onActionFlow<TestAction> {
                        flowOf(1)
                    }
                }
            }
        }

        @Test
        fun `given action handled in onActionFlow, when action submitted twice, then action handled twice`() =
            runTest {
                val sut = createSut()
                sut.testState(this) {
                    sut.actions.onActionFlow<TestAction.QueryChanged> {
                        map { "test ${it.query}" }
                            .onEach { newValue ->
                                sut.setState { copy(value = newValue) }
                            }
                    }

                    submitAction(TestAction.QueryChanged("query1"))
                    assertEquals("test query1", expectMostRecentItem().value)

                    submitAction(TestAction.QueryChanged("query2"))
                    assertEquals("test query2", expectMostRecentItem().value)
                }
            }

        @Test
        fun `given action handled in onActionFlowSingle, when action submitted twice, then action handled only once`() =
            runTest {
                val sut = createSut()
                sut.testState(this) {
                    sut.actions.onActionFlowSingle<TestAction.QueryChanged> { queryChanged ->
                        flowOf("test")
                            .map { "$it ${queryChanged.query}" }
                            .onEach { newValue ->
                                sut.setState { copy(value = newValue) }
                            }
                    }

                    submitAction(TestAction.QueryChanged("query1"))
                    assertEquals("test query1", expectMostRecentItem().value)

                    submitAction(TestAction.QueryChanged("query2"))
                    expectNoEvents()
                }
            }

        @Test
        fun `given action handled in onAction, when action submitted twice, then action handled twice`() =
            runTest {
                val sut = createSut()
                sut.testState(this) {
                    sut.actions.onAction<TestAction.QueryChanged> { queryChanged ->
                        sut.setState { copy(value = queryChanged.query) }
                    }

                    submitAction(TestAction.QueryChanged("query1"))
                    assertEquals("query1", expectMostRecentItem().value)

                    submitAction(TestAction.QueryChanged("query2"))
                    assertEquals("query2", expectMostRecentItem().value)
                }
            }

        @Test
        fun `given action handled in onActionSingle, when action submitted twice, then action only one`() =
            runTest {
                val sut = createSut()
                sut.testState(this) {
                    sut.actions.onActionSingle<TestAction.QueryChanged> { queryChanged ->
                        sut.setState { copy(value = queryChanged.query) }
                    }

                    submitAction(TestAction.QueryChanged("query1"))
                    assertEquals("query1", expectMostRecentItem().value)

                    submitAction(TestAction.QueryChanged("query2"))
                    expectNoEvents()
                }
            }
    }

    @Nested
    inner class ExceptionHandler {
        @Test
        fun `given exception handler, when unhandled error in onActionSingle, then notify exception handler`() =
            runTest {
                var caughtError: Throwable? = null
                val testError = IllegalStateException("test1")
                val sut = createSut(
                    exceptionHandler = CoroutineExceptionHandler { _, exception ->
                        caughtError = exception
                    }
                )
                sut.actions.onActionSingle<TestAction.QueryChanged> {
                    throw testError
                }

                sut.submitAction(TestAction.QueryChanged("query1"))

                assertEquals(testError, caughtError)
            }

        @Test
        fun `given exception handler, when unhandled error in onAction, then notify exception handler`() =
            runTest {
                var caughtError: Throwable? = null
                val testError = IllegalStateException("test2")
                val sut = createSut(
                    exceptionHandler = CoroutineExceptionHandler { _, exception ->
                        caughtError = exception
                    }
                )
                sut.actions.onAction<TestAction.QueryChanged> {
                    throw testError
                }

                sut.submitAction(TestAction.QueryChanged("query1"))

                assertEquals(testError.message, caughtError?.message)
                assertIs<IllegalStateException>(caughtError)
            }

        @Test
        fun `given exception handler, when unhandled error in onActionFlowSingle, then notify exception handler`() =
            runTest {
                var caughtError: Throwable? = null
                val testError = IllegalStateException("test3")
                val sut = createSut(
                    exceptionHandler = CoroutineExceptionHandler { _, exception ->
                        caughtError = exception
                    }
                )
                sut.actions.onActionFlowSingle<TestAction.QueryChanged> {
                    flow {
                        emit(1)
                        throw testError
                    }
                }

                sut.submitAction(TestAction.QueryChanged("query1"))

                assertEquals(testError.message, caughtError?.message)
                assertIs<IllegalStateException>(caughtError)
            }

        @Test
        fun `given exception handler, when unhandled error in onActionFlow, then notify exception handler`() =
            runTest {
                var caughtError: Throwable? = null
                val testError = IllegalStateException("test4")
                val sut = createSut(
                    exceptionHandler = CoroutineExceptionHandler { _, exception ->
                        caughtError = exception
                    }
                )
                sut.actions.onActionFlow<TestAction.QueryChanged> {
                    mapLatest {
                        throw testError
                    }
                }

                sut.submitAction(TestAction.QueryChanged("query1"))

                assertEquals(testError.message, caughtError?.message)
                assertIs<IllegalStateException>(caughtError)
            }
    }

    @Nested
    inner class Lifecycle {

        @Test
        fun `when first state subscriber, then call onInit`() = runTest {
            val sut = createSut()
            var called = false
            val job = launch {
                sut.onInit {
                    called = true
                }
            }

            assertFalse(called)
            sut.currentState.launchIn(backgroundScope)

            job.join()
            assertTrue(called)
        }

        @Test
        fun `verify state has initially one subscriber`() = runTest {
            createSut().subscribersCount.test {
                assertEquals(1, awaitItem())
            }
        }

        @Test
        fun `when subscribed, then call onSubscribe, when unsubscribe, then call onUnsubscribe`() = runTest {
            val sut = createSut()
            var subscribeCount = 0
            var unsubscribeCount = 0

            launch {
                sut.onSubscribe {
                    subscribeCount++
                }

                sut.onUnsubscribe {
                    unsubscribeCount++
                }
            }
            advanceUntilIdle()

            assertEquals(0, subscribeCount)
            assertEquals(0, unsubscribeCount)

            val job1 = launch {
                sut.currentState.collect {  }
            }
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(0, unsubscribeCount)

            val job2 = launch {
                sut.currentState.collect {  }
            }
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(0, unsubscribeCount)

            job1.cancel()
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(0, unsubscribeCount)

            job2.cancel()
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(1, unsubscribeCount)

            val job3 = launch {
                sut.currentState.collect {  }
            }
            advanceUntilIdle()

            assertEquals(2, subscribeCount)
            assertEquals(1, unsubscribeCount)

            job3.cancel()
            advanceUntilIdle()

            assertEquals(2, subscribeCount)
            assertEquals(2, unsubscribeCount)
        }

    }

    @Test
    fun `when clear called, then scope cancelled`() = runTest {
        val sut = createSut()
        assertTrue(sut.scope.isActive)

        sut.clear()

        assertFalse(sut.scope.isActive)
    }

    private sealed interface TestEffect : MviEffect {
        data class Navigate(val route: String) : TestEffect
        data object Refresh : TestEffect
    }

    private sealed interface TestAction : MviAction {
        data class QueryChanged(val query: String) : TestAction
    }

    private data class TestState(val value: String = "") : MVIState

    private class NoOpLogger : Logger {

        override fun onAction(action: MviAction) {
            println("Action: $action")
        }

        override fun onInit() {
            println("onInit")
        }

        override fun onSubscribe() {
            println("onSubscribe")
        }

        override fun onUnsubscribe() {
            println("onUnsubscribe")
        }

        override fun onEffect(effect: MviEffect) {
            println("onEffect: $effect")
        }

        override fun onInitialState(state: MVIState) {
            println("onInitialState $state")
        }

        override fun onState(state: MVIState) {
            println("onState: $state")
        }

        override fun onClear() {
            println("onClear")
        }
    }
}
