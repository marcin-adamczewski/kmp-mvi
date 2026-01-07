package com.adamczewski.kmpmvi.mvi

import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager.ActionNotSubscribedException
import com.adamczewski.kmpmvi.mvi.logger.DefaultMviLogger
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import com.adamczewski.kmpmvi.test.testEffects
import com.adamczewski.kmpmvi.test.testState
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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MviContainerTest {

    @AfterTest
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    private val scopeProvider = { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    private fun createSut(
        initialState: TestState = TestState(),
        effectsBufferSize: Int = 10,
        exceptionHandler: CoroutineExceptionHandler? = null,
        scope: CoroutineScope? = null,
    ): MviContainer<TestAction, TestState, TestEffect> {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        return MviContainer<TestAction, TestState, TestEffect>(
            scopeProvider = { scope ?: scopeProvider() },
            initialState = initialState,
            settings = MviSettings(
                isLoggerEnabled = true,
                logger = { DefaultMviLogger("MviContainerTest") },
                effectsBufferSize = effectsBufferSize,
                exceptionHandler = exceptionHandler,
                scopeProvider = { scope ?: scopeProvider() }
            )
        )
    }

    @Test
    fun `Effects - when emitting effects before any subscription then emit all effects when subscribed`() =
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
    fun `Effects - when emitting effects after subscription then emit all effects when subscribed`() =
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
    fun `Effects - when emitted effects consumed then effects not repeated between subscriptions`() =
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
    fun `Effects - when observing effects without consuming then effects repeated on the next subscription`() =
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
    fun `Effects - when observing and consuming effects in the same time then effects multicasted to both subscribers`() =
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
    fun `Effects - when effect was already consumed then new subscriber receives only new effects`() =
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
    fun `Effects - when not all effects are consumed then unconsumed effects are emitted on the next subscription`() =
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
    fun `Effects - when same effects emitted twice in a row then receive them twice`() =
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
    fun `Effects - when effects count is over buffer size then emit only last effects that fits the buffer`() =
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
    fun `Effects - given no active consumer when emitting effects with required active consumer then do not emit effects when consumer available`() =
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
    fun `Effects - given no active consumer when consumeEffectFlow called then emit only effects that do not require active consumer`() =
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
    fun `Effects - given active consumer when emitting effects with required active consumer then effects emitted`() =
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

    @Test
    fun `Actions - when flow passed to onActionFlow then throw error`() {
        assertFailsWith<ActionNotSubscribedException> {
            runTest {
                createSut().handleActions {
                    onActionFlow<TestAction> {
                        flowOf(1)
                    }
                }
            }
        }
    }

    @Test
    fun `Actions - when action submitted before subscribers then postpone emitting action until subscribers`() =
        runTest {
            val sut = createSut()
            sut.submitAction(TestAction.QueryChanged("query"))
            sut.submitAction(TestAction.Refresh)

            sut.handleActions {
                onActionFlow<TestAction.QueryChanged> {
                    mapLatest { sut.setState { copy(value = it.query) } }
                }

                onAction<TestAction.Refresh> {
                    sut.setState { copy(refreshed = true) }
                }
            }

            sut.testState(this) {
                assertEquals(
                    TestState(value = "query", refreshed = true),
                    expectMostRecentItem()
                )
            }
        }

    @Test
    fun `Actions - given action handled in onActionFlow when action submitted twice then action handled twice`() =
        runTest {
            val sut = createSut()
            sut.testState(this) {
                sut.handleActions {
                    onActionFlow<TestAction.QueryChanged> {
                        map { "test ${it.query}" }
                            .onEach { newValue ->
                                sut.setState { copy(value = newValue) }
                            }
                    }
                }

                submitAction(TestAction.QueryChanged("query1"))
                assertEquals("test query1", expectMostRecentItem().value)

                submitAction(TestAction.QueryChanged("query2"))
                assertEquals("test query2", expectMostRecentItem().value)
            }
        }

    @Test
    fun `Actions - given action handled in onActionFlowSingle when action submitted twice then action handled only once`() =
        runTest {
            val sut = createSut()
            sut.testState(this) {
                sut.handleActions {
                    onActionFlowSingle<TestAction.QueryChanged> { queryChanged ->
                        flowOf("test")
                            .map { "$it ${queryChanged.query}" }
                            .onEach { newValue ->
                                sut.setState { copy(value = newValue) }
                            }
                    }
                }

                submitAction(TestAction.QueryChanged("query1"))
                assertEquals("test query1", expectMostRecentItem().value)

                submitAction(TestAction.QueryChanged("query2"))
                expectNoEvents()
            }
        }

    @Test
    fun `Actions - given action handled in onAction when action submitted twice then action handled twice`() =
        runTest {
            val sut = createSut()
            sut.testState(this) {
                sut.handleActions {
                    onAction<TestAction.QueryChanged> { queryChanged ->
                        sut.setState { copy(value = queryChanged.query) }
                    }
                }

                submitAction(TestAction.QueryChanged("query1"))
                assertEquals("query1", expectMostRecentItem().value)

                submitAction(TestAction.QueryChanged("query2"))
                assertEquals("query2", expectMostRecentItem().value)
            }
        }

    @Test
    fun `Actions - given action handled in onActionSingle when action submitted twice then action only one`() =
        runTest {
            val sut = createSut()
            sut.testState(this) {
                sut.handleActions {
                    onActionSingle<TestAction.QueryChanged> { queryChanged ->
                        sut.setState { copy(value = queryChanged.query) }
                    }
                }

                submitAction(TestAction.QueryChanged("query1"))
                assertEquals("query1", expectMostRecentItem().value)

                submitAction(TestAction.QueryChanged("query2"))
                expectNoEvents()
            }
        }

    @Test
    fun `ExceptionHandler - given exception handler when unhandled error in onActionSingle then notify exception handler`() =
        runTest {
            var caughtError: Throwable? = null
            val testError = IllegalStateException("test1")
            val sut = createSut(
                exceptionHandler = CoroutineExceptionHandler { _, exception ->
                    caughtError = exception
                }
            )
            sut.handleActions {
                onActionSingle<TestAction.QueryChanged> {
                    throw testError
                }
            }

            sut.submitAction(TestAction.QueryChanged("query1"))

            assertEquals(testError, caughtError)
        }

    @Test
    fun `ExceptionHandler - given exception handler when unhandled error in onAction then notify exception handler`() =
        runTest {
            var caughtError: Throwable? = null
            val testError = IllegalStateException("test2")
            val sut = createSut(
                exceptionHandler = CoroutineExceptionHandler { _, exception ->
                    caughtError = exception
                }
            )
            sut.handleActions {
                onAction<TestAction.QueryChanged> {
                    throw testError
                }
            }

            sut.submitAction(TestAction.QueryChanged("query1"))

            assertEquals(testError.message, caughtError?.message)
            assertIs<IllegalStateException>(caughtError)
        }

    @Test
    fun `ExceptionHandler - given exception handler when unhandled error in onActionFlowSingle then notify exception handler`() =
        runTest {
            var caughtError: Throwable? = null
            val testError = IllegalStateException("test3")
            val sut = createSut(
                exceptionHandler = CoroutineExceptionHandler { _, exception ->
                    caughtError = exception
                }
            )
            sut.handleActions {
                onActionFlowSingle<TestAction.QueryChanged> {
                    flow {
                        emit(1)
                        throw testError
                    }
                }
            }

            sut.submitAction(TestAction.QueryChanged("query1"))

            assertEquals(testError.message, caughtError?.message)
            assertIs<IllegalStateException>(caughtError)
        }

    @Test
    fun `ExceptionHandler - given exception handler when unhandled error in onActionFlow then notify exception handler`() =
        runTest {
            var caughtError: Throwable? = null
            val testError = IllegalStateException("test4")
            val sut = createSut(
                exceptionHandler = CoroutineExceptionHandler { _, exception ->
                    caughtError = exception
                }
            )
            sut.handleActions {
                onActionFlow<TestAction.QueryChanged> {
                    mapLatest {
                        throw testError
                    }
                }
            }

            sut.submitAction(TestAction.QueryChanged("query1"))

            assertEquals(testError.message, caughtError?.message)
            assertIs<IllegalStateException>(caughtError)
        }

    @Test
    fun `Lifecycle - verify state has initially zero subscribers`() = runTest {
        createSut().subscribersCount.test {
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun `Lifecycle - when first state subscriber then call onInit`() = runTest {
        val sut = createSut()
        var called = false
        val job = launch {
            sut.onInit {
                called = true
            }
        }

        assertFalse(called)
        sut.state.launchIn(backgroundScope)

        job.join()
        assertTrue(called)
    }

    @Test
    fun `Lifecycle - when subscribed then call onSubscribe when unsubscribe then call onUnsubscribe`() =
        runTest {
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
                sut.state.collect {}
            }
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(0, unsubscribeCount)

            val job2 = launch {
                sut.state.collect { }
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
                sut.state.collect { }
            }
            advanceUntilIdle()

            assertEquals(2, subscribeCount)
            assertEquals(1, unsubscribeCount)

            job3.cancel()
            advanceUntilIdle()

            assertEquals(2, subscribeCount)
            assertEquals(2, unsubscribeCount)
        }

    @Test
    fun `Lifecycle - when subscribed to non-lifecycle state then do not call callbacks and do not increase subscribers count`() =
        runTest {
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

            var collected = false
            val job1 = launch {
                sut.observableState.collect {
                    collected = true
                }
            }
            advanceUntilIdle()

            assertTrue(collected)
            sut.subscribersCount.test {
                assertEquals(0, awaitItem())
                assertEquals(0, subscribeCount)
                assertEquals(0, unsubscribeCount)
                job1.cancel()
            }
        }

    @Test
    fun `Lifecycle - given state subscribed when subscribed to non-lifecycle state then do not increase subscribers count`() =
        runTest {
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
            val job1 = launch {
                sut.state.collect {}
            }
            advanceUntilIdle()

            var collected = false
            val job2 = launch {
                sut.observableState.collect {
                    collected = true
                }
            }
            advanceUntilIdle()

            assertTrue(collected)
            sut.subscribersCount.test {
                assertEquals(1, awaitItem())
                assertEquals(1, subscribeCount)
                assertEquals(0, unsubscribeCount)
                job1.cancel()
                job2.cancel()
                assertEquals(0, awaitItem())
                assertEquals(1, unsubscribeCount)
            }
        }

    @Test
    fun `when clear called then scope cancelled`() = runTest {
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
        data object Refresh : TestAction
    }

    private data class TestState(
        val value: String = "",
        val refreshed: Boolean = false
    ) : MviState

}
