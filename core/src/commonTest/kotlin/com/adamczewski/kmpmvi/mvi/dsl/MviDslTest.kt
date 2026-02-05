package com.adamczewski.kmpmvi.mvi.dsl

import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.error.BaseErrorManager
import com.adamczewski.kmpmvi.mvi.error.LONG_ERROR_DURATION
import com.adamczewski.kmpmvi.mvi.error.UiError
import com.adamczewski.kmpmvi.mvi.error.toUiError
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.progress.watchProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MviDslTest {

    @BeforeTest
    fun beforeEach() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when action submitted, then action hanlded and state updated`() = runTest {
        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            actions {
                onAction<TestAction.UpdateValue> { action ->
                    setState { copy(value = action.value) }
                }
            }
        }

        sut.lifecycleState.test {
            assertEquals("", awaitItem().value)
            sut.submitAction(TestAction.UpdateValue("new value"))
            assertEquals("new value", awaitItem().value)
        }
    }

    @Test
    fun `given single action, when action submitted, then action hanlded and state updated once`() =
        runTest {
            val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
                actions {
                    onActionSingle<TestAction.UpdateValue> { action ->
                        setState { copy(value = action.value) }
                    }
                }
            }

            sut.lifecycleState.test {
                assertEquals("", awaitItem().value)
                sut.submitAction(TestAction.UpdateValue("new value"))
                sut.submitAction(TestAction.UpdateValue("new value1"))
                sut.submitAction(TestAction.UpdateValue("new value2"))
                assertEquals("new value", awaitItem().value)
                expectNoEvents()
            }
        }

    @Test
    fun `given flow action, when action submitted, then action hanlded and state updated`() =
        runTest {
            val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
                actions {
                    onActionFlow<TestAction.UpdateValue> {
                        mapLatest {
                            setState { copy(value = it.value) }
                        }

                    }
                }
            }

            sut.lifecycleState.test {
                assertEquals("", awaitItem().value)
                sut.submitAction(TestAction.UpdateValue("new value"))
                assertEquals("new value", awaitItem().value)
            }
        }

    @Test
    fun `given flow single action, when action submitted, then action hanlded and state updated once`() =
        runTest {
            val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
                actions {
                    onActionFlowSingle<TestAction.UpdateValue> {
                        flow<Unit> {
                            setState { copy(value = it.value) }
                        }
                    }
                }
            }

            sut.lifecycleState.test {
                assertEquals("", awaitItem().value)
                sut.submitAction(TestAction.UpdateValue("new value"))
                sut.submitAction(TestAction.UpdateValue("new value1"))
                sut.submitAction(TestAction.UpdateValue("new value2"))
                assertEquals("new value", awaitItem().value)
                expectNoEvents()
            }
        }

    @Test
    fun `when action handled, then effect emitted from action handler`() = runTest {
        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            actions {
                onAction<TestAction.EmitEffect> {
                    setEffect { TestEffect.SomeEffect("effect data") }
                }
            }
        }

        sut.effects.observeEffects.test {
            sut.submitAction(TestAction.EmitEffect)
            assertEquals(TestEffect.SomeEffect("effect data"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `when action with progress, then observing progress updates progress`() = runTest {
        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            observeProgress { isLoading ->
                setState { copy(isLoading = isLoading) }
            }

            actions {
                onAction<TestAction.UpdateValue> {
                    scope.launch {
                        withProgress {
                            delay(100)
                            setState { copy(value = "processed") }
                        }
                    }
                }
            }
        }

        sut.lifecycleState.test {
            assertFalse(awaitItem().isLoading)

            sut.submitAction(TestAction.UpdateValue("test"))
            advanceTimeBy(50)
            assertTrue(awaitItem().isLoading)

            advanceTimeBy(51)

            assertFalse(expectMostRecentItem().isLoading)
        }
    }

    @Test
    fun `when flow action with progress, then observing progress updates progress`() = runTest {
        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            observeProgress { isLoading ->
                setState { copy(isLoading = isLoading) }
            }

            actions {
                onActionFlow<TestAction.UpdateValue> {
                    flatMapLatest {
                        flow {
                            delay(100)
                            emit(1)
                        }.watchProgress(progress)
                    }.onEach {
                        setState { copy(value = "processed") }
                    }
                }
            }
        }

        sut.lifecycleState.test {
            assertFalse(awaitItem().isLoading)

            sut.submitAction(TestAction.UpdateValue("test"))
            advanceTimeBy(50)
            assertTrue(awaitItem().isLoading)

            advanceTimeBy(51)

            assertFalse(expectMostRecentItem().isLoading)
        }
    }

    @Test
    fun `when lifecycleState subscribed then, onInit callback called`() = runTest {
        var onInitCalled = false
        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            onInit {
                onInitCalled = true
            }
        }

        assertFalse(onInitCalled)
        sut.lifecycleState.test {
            advanceUntilIdle()
            assertTrue(onInitCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when observableState subscribed then, onInit callback not called`() = runTest {
        var onInitCalled = false
        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            onInit {
                onInitCalled = true
            }
        }

        sut.observableState.test {
            advanceUntilIdle()
            assertFalse(onInitCalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given error observed, when error emitted, then error updated in state`() = runTest {
        val errorManager = ErrorManager()
        val error = Throwable("test").toUiError(LONG_ERROR_DURATION)

        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            observeError(errorManager) { error ->
                setState { copy(error = error) }
            }

            actions {
                onAction<TestAction.UpdateValue> {
                    scope.launch {
                        errorManager.addError(error)
                    }
                }
            }
        }

        sut.lifecycleState.test {
            assertNull(awaitItem().error)

            sut.submitAction(TestAction.UpdateValue("test"))

            assertEquals(error, awaitItem().error)

            advanceTimeBy(LONG_ERROR_DURATION)

            assertNull(awaitItem().error)
        }
    }

    private data class TestState(
        val value: String = "",
        val isLoading: Boolean = false,
        val error: UiError? = null,
    ) : MviState

    private sealed interface TestAction : MviAction {
        data class UpdateValue(val value: String) : TestAction
        data object EmitEffect : TestAction
    }
    private sealed interface TestEffect : MviEffect {
        data class SomeEffect(val data: String) : TestEffect
    }
}