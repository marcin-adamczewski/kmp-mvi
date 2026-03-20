package com.adamczewski.kmpmvi.mvi

import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.actions.ActionNotSubscribedException
import com.adamczewski.kmpmvi.mvi.logger.DefaultMviLogger
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.progress.ProgressManager
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import com.adamczewski.kmpmvi.test.testEffects
import com.adamczewski.kmpmvi.test.testMessages
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
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
    ): BaseMviContainer<TestAction, TestState, TestEffect, TestMessage> {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        return BaseMviContainer<TestAction, TestState, TestEffect, TestMessage>(
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

    private fun createSealedStateSut(
        initialState: SealedTestState = SealedTestState.Loading,
        effectsBufferSize: Int = 10,
        exceptionHandler: CoroutineExceptionHandler? = null,
        scope: CoroutineScope? = null,
    ): MviContainer<TestAction, SealedTestState, TestEffect> {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        return MviContainer<TestAction, SealedTestState, TestEffect>(
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
    fun `State - when initial state set then state emitted to subscribers`() = runTest {
        val sut = createSut(initialState = TestState(value = "initial"))
        sut.testState(this) {
            assertEquals(TestState(value = "initial"), awaitItem())
            expectNoEvents()
            cancel()
        }
    }

    @Test
    fun `State - when state set then state emitted to subscribers`() = runTest {
        val sut = createSut()
        sut.testState(this) {
            assertEquals(TestState(), awaitItem())

            sut.setState { copy(value = "test") }

            assertEquals(TestState(value = "test", refreshed = false), awaitItem())

            sut.setState { copy(value = "test2", refreshed = true) }
            sut.setState { copy(value = "test3", refreshed = false) }

            assertEquals(TestState(value = "test2", refreshed = true), awaitItem())
            assertEquals(TestState(value = "test3", refreshed = false), awaitItem())
        }
    }

    @Test
    fun `State - when sealed state updated then updated state emitted to subscribers`() = runTest {
        val sut = createSealedStateSut(initialState = SealedTestState.Loading)
        sut.testState(this) {
            assertEquals(SealedTestState.Loading, awaitItem())

            sut.setState { SealedTestState.Data(id = "id") }

            assertEquals(SealedTestState.Data(id = "id"), awaitItem())

            sut.updateState<SealedTestState.Data> { SealedTestState.Data(id = "id2") }

            assertEquals(SealedTestState.Data(id = "id2"), awaitItem())
        }
    }

    @Test
    fun `State - given loading state when non-present data state updated then state is not updated`() = runTest {
        val sut = createSealedStateSut(initialState = SealedTestState.Loading)
        sut.testState(this) {
            assertEquals(SealedTestState.Loading, awaitItem())

            // We can't update Data state as the current state is Loading
            sut.updateState<SealedTestState.Data> { SealedTestState.Data(id = "id2") }

            expectNoEvents()
            cancel()
        }
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
    fun `Actions - when flow passed to onActionFlow then throw error when action emitted`() = runTest {
        var caughtError: Throwable? = null
        val sut = createSut(
            exceptionHandler = CoroutineExceptionHandler { _, exception ->
                caughtError = exception
            }
        )
        sut.handleActions {
            onActionFlow<TestAction> {
                flowOf(1)
            }

            submitAction(TestAction.Refresh)
        }
        advanceUntilIdle()

        assertIs<ActionNotSubscribedException>(caughtError)
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
    fun `Messages - given observing messages when messages emitted then messages received`() = runTest {
        val sut = createSut()
        sut.testMessages(this) {
            sut.messenger.setMessage(TestMessage.InitializedMessage)
            sut.messenger.setMessage(TestMessage.InitializedMessage)
            sut.messenger.setMessage(TestMessage.DataMessage("123"))

            assertEquals(TestMessage.InitializedMessage, awaitItem())
            assertEquals(TestMessage.InitializedMessage, awaitItem())
            assertEquals(TestMessage.DataMessage("123"), awaitItem())
        }
    }

    @Test
    fun `Messages - when observing messages after first message emitted then first message not received`() = runTest {
        val sut = createSut()
        sut.messenger.setMessage(TestMessage.InitializedMessage)
        sut.testMessages(this) {
            sut.messenger.setMessage(TestMessage.DataMessage("1"))
            sut.messenger.setMessage(TestMessage.DataMessage("2"))

            assertEquals(TestMessage.DataMessage("1"), awaitItem())
            assertEquals(TestMessage.DataMessage("2"), awaitItem())
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
    fun `Lifecycle - verify effects handler has initially zero consumers`() = runTest {
        createSut().effects.activeConsumers.test {
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
        sut.lifecycleState.launchIn(backgroundScope)

        job.join()
        assertTrue(called)
    }

    @Test
    fun `Lifecycle - when first effects consumer active then call onInit`() = runTest {
        val sut = createSut()
        var called = false
        val job = launch {
            sut.onInit {
                called = true
            }
        }

        assertFalse(called)
        backgroundScope.launch {
            sut.effects.consume {}
        }

        job.join()
        assertTrue(called)
    }

    // This verifies design decision to not call onInit when single effect is collected.
    // As single effect may be consumed inside other ViewModels, not necessarily in UI.
    @Test
    fun `Lifecycle - when first single-effect flow consumer active then do not call onInit`() = runTest {
        val sut = createSut()
        var called = false
        var collected = false
        val job = launch {
            sut.onInit {
                called = true
            }
        }

        backgroundScope.launch {
            sut.effects.consumeEffectFlow<TestEffect.Refresh> {}.collect {
                collected = true
            }
        }
        sut.setEffect { TestEffect.Refresh }

        job.join()
        assertFalse(called)
        assertTrue(collected)
    }

    @Test
    fun `Lifecycle - when first effects flow consumer active then call onInit`() = runTest {
        val sut = createSut()
        var called = false
        val job = launch {
            sut.onInit {
                called = true
            }
        }

        assertFalse(called)
        backgroundScope.launch {
            sut.effects.consume {}
        }

        job.join()
        assertTrue(called)
    }


    @Test
    fun `Lifecycle - when state and effects subscribed then call onInit once`() = runTest {
        val sut = createSut()
        var onInitCount = 0
        var effectConsumed = false
        var stateCollected = false
        val job = launch {
            sut.onInit {
                onInitCount++
            }
        }
        assertEquals(0, onInitCount)
        sut.setEffect { TestEffect.Refresh }

        sut.effects
            .consumeFlow {
                effectConsumed = true
            }
            .launchIn(backgroundScope)
        sut.lifecycleState
            .onEach { stateCollected = true }
            .launchIn(backgroundScope)

        job.join()
        advanceUntilIdle()
        assertEquals(1, onInitCount)
        assertTrue(effectConsumed)
        assertTrue(stateCollected)
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
                sut.lifecycleState.collect {}
            }
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(0, unsubscribeCount)

            val job2 = launch {
                sut.lifecycleState.collect { }
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
                sut.lifecycleState.collect { }
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
    fun `Lifecycle - when consumed effects then call onSubscribe when unsubscribe then call onUnsubscribe`() =
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
                sut.effects.consume {}
            }
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(0, unsubscribeCount)

            val job2 = launch {
                sut.effects.consume {}
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
                sut.effects.consume {}
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
    fun `Lifecycle - when subscribed to state and consumed effects then call onSubscribe once - when unsubscribe then call onUnsubscribe once`() =
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

            val jobEffects1 = launch {
                sut.effects.consume {}
            }
            val jobState1 = launch {
                sut.lifecycleState.collect {}
            }
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(0, unsubscribeCount)

            val jobEffects2 = launch {
                sut.effects.consume {}
            }
            val jobState2 = launch {
                sut.lifecycleState.collect {}
            }
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(0, unsubscribeCount)

            jobEffects1.cancel()
            jobState1.cancel()
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(0, unsubscribeCount)

            jobEffects2.cancel()
            jobState2.cancel()
            advanceUntilIdle()

            assertEquals(1, subscribeCount)
            assertEquals(1, unsubscribeCount)

            val jobEffects3 = launch {
                sut.effects.consume {}
            }
            val jobState3 = launch {
                sut.lifecycleState.collect {}
            }
            advanceUntilIdle()

            assertEquals(2, subscribeCount)
            assertEquals(1, unsubscribeCount)

            jobEffects3.cancel()
            jobState3.cancel()
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
    fun `Lifecycle - when obserbing effects then do not call callbacks and do not increase effect collectors count`() =
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
            sut.setEffect { TestEffect.Refresh }

            var collected = false
            val job1 = launch {
                sut.effects.observeEffects.collect {
                    collected = true
                }
            }
            advanceUntilIdle()

            assertTrue(collected)
            sut.effects.activeConsumers.test {
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
                sut.lifecycleState.collect {}
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
    fun `Progress - when observing progress then initially no value is returned`() =
        runTest {
            var valueEmitted: Boolean = false
            val sut = createSut()
            sut.observeProgress { isLoading ->
                valueEmitted = true
            }

            advanceUntilIdle()
            assertFalse { valueEmitted }
        }

    @Test
    fun `Progress - when observing mutliple progresses then initially no value is returned`() =
        runTest {
            var valueEmitted: Boolean = false
            val sut = createSut()
            val progress2 = ProgressManager()
            sut.observeProgress(sut.progress, progress2) { isLoading ->
                valueEmitted = true
            }

            advanceUntilIdle()
            assertFalse { valueEmitted }
        }

    @Test
    fun `Progress - given observing progress when progress changed then progress updateds received`() =
        runTest {
            var loadingState = false
            val sut = createSut()
            sut.observeProgress { isLoading ->
                loadingState = isLoading
            }

            sut.progress.addProgress("id1")
            advanceUntilIdle()

            assertTrue(loadingState)

            sut.progress.addProgress("id2")
            advanceUntilIdle()

            assertTrue(loadingState)

            sut.progress.removeProgress("id1")
            advanceUntilIdle()

            assertTrue(loadingState)

            sut.progress.removeProgress("id2")
            advanceUntilIdle()

            assertFalse(loadingState)
        }

    @Test
    fun `Progress - given observing multiple progresses when progress changed then combined progress states updateds received`() =
        runTest {
            var loadingState = false
            val sut = createSut()
            val progress2 = ProgressManager()
            sut.observeProgress(sut.progress, progress2) { isLoading ->
                loadingState = isLoading
            }

            sut.progress.addProgress("id1")
            advanceUntilIdle()

            assertTrue(loadingState)

            progress2.addProgress("id1")
            advanceUntilIdle()

            assertTrue(loadingState)

            sut.progress.removeProgress("id1")
            advanceUntilIdle()

            assertTrue(loadingState)

            progress2.removeProgress("id1")
            advanceUntilIdle()

            assertFalse(loadingState)
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

    private sealed interface SealedTestState : MviState {
        data object Loading : SealedTestState
        data object Error : SealedTestState
        data class Data(val id: String) : SealedTestState
    }

    private sealed interface TestMessage : MviMessage {
        data object InitializedMessage : TestMessage
        data class DataMessage(val data: String) : TestMessage
    }
}
