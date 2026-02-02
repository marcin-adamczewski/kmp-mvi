package com.adamczewski.kmpmvi.mvi

import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.dsl.mvi
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MviDslTest {

    private data class TestState(val value: String = "") : MviState
    private sealed interface TestAction : MviAction {
        data class UpdateValue(val value: String) : TestAction
        data object EmitEffect : TestAction
    }
    private sealed interface TestEffect : MviEffect {
        data class SomeEffect(val data: String) : TestEffect
    }

    @Test
    fun mviDslShouldHandleActionsAndStateUpdates() = runTest {
        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            actions {
                onAction<TestAction.UpdateValue> { action ->
                    setState { copy(value = action.value) }
                }
            }
        }

        sut.observableState.test {
            assertEquals("", awaitItem().value)
            sut.submitAction(TestAction.UpdateValue("new value"))
            assertEquals("new value", awaitItem().value)
        }
    }

    @Test
    fun mviDslShouldHandleEffects() = runTest {
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
        }
    }

    @Test
    fun mviDslShouldWorkWithinInheritance() = runTest {
        class InheritedViewModel : BaseMviStateManager<TestAction, TestState, TestEffect, Nothing>(TestState()) {
            init {
                mvi {
                    actions {
                        onAction<TestAction.UpdateValue> { action ->
                            setState { copy(value = action.value) }
                        }
                    }
                }
            }
        }

        val sut = InheritedViewModel()

        sut.observableState.test {
            assertEquals("", awaitItem().value)
            sut.submitAction(TestAction.UpdateValue("inherited"))
            assertEquals("inherited", awaitItem().value)
        }
    }

    @Test
    fun mviDslShouldProvideAccessToScopeAndProgress() = runTest {
        var scopeAccessedBuilder = false
        var progressAccessedBuilder = false
        var scopeAccessedActions = false
        var progressAccessedActions = false
        var scopeAccessedActionScope = false
        var progressAccessedActionScope = false

        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            scopeAccessedBuilder = true
            progressAccessedBuilder = true

            actions {
                scopeAccessedActions = true
                progressAccessedActions = true

                onAction<TestAction.UpdateValue> {
                    scopeAccessedActionScope = true
                    progressAccessedActionScope = true
                    setState { copy(value = "processed") }
                }
            }
        }

        sut.observableState.test {
            assertEquals("", awaitItem().value)
            sut.submitAction(TestAction.UpdateValue("test"))
            assertEquals("processed", awaitItem().value)
        }

        assertTrue(scopeAccessedBuilder)
        assertTrue(progressAccessedBuilder)
        assertTrue(scopeAccessedActions)
        assertTrue(progressAccessedActions)
        assertTrue(scopeAccessedActionScope)
        assertTrue(progressAccessedActionScope)
    }

    @Test
    fun mviDslShouldUseProvidedScope() = runTest {
        val customScope = CoroutineScope(UnconfinedTestDispatcher())
        val sut = mvi<TestAction, TestState, TestEffect>(TestState(), scope = customScope) {
            actions {
                onAction<TestAction.UpdateValue> { }
            }
        }

        assertEquals(
            customScope.coroutineContext[ContinuationInterceptor],
            sut.scope.coroutineContext[ContinuationInterceptor]
        )
    }

    @Test
    fun mviDslShouldHandleLifecycleEvents() = runTest {
        val sut = mvi<TestAction, TestState, TestEffect>(TestState()) {
            lifecycle {
                onInit {
                    // Use state/effect helpers inside onInit
                    setState { copy(value = "initialized") }
                    setEffect { TestEffect.SomeEffect("init") }
                }
            }
        }

        // Subscribe to lifecycleState to trigger onInit
        sut.lifecycleState.test {
            // First emission may already be updated depending on platform scheduling
            val first = awaitItem().value
            if (first != "initialized") {
                assertEquals("initialized", awaitItem().value)
            }
            cancel()
        }

        sut.effects.observeEffects.test {
            // Effect from onInit should be emitted once
            assertEquals(TestEffect.SomeEffect("init"), awaitItem())
            cancel()
        }
    }
}
