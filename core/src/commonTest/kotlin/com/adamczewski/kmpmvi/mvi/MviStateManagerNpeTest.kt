package com.adamczewski.kmpmvi.mvi

import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.test.testState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MviStateManagerNpeTest {

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ensure that child fields are accessible in the onAction call`() = runTest {
        NpeStateManager().testState(this) {
            submitAction(TestAction.Init)
            assertEquals("test", expectMostRecentItem().value)
        }
        advanceUntilIdle()
    }

    class NpeStateManager : BaseMviStateManager<TestAction, TestState, TestEffect, Nothing>(
        initialState = TestState()
    ) {
        private val someFlowAssignedInTheField = MutableSharedFlow<String>(replay = 1)

        init {
            someFlowAssignedInTheField.tryEmit("test")
        }

        override fun ActionsManager<TestAction>.handleActions() {
            onActionFlow<TestAction.Init> {
                assertNotNull(someFlowAssignedInTheField)
                flatMapLatest { someFlowAssignedInTheField }
                    .onEach { setState { copy(value = it) } }
            }
        }
    }

    sealed class TestAction : MviAction {
        object Init : TestAction()
    }
    data class TestState(val value: String = "") : MviState
    sealed class TestEffect : MviEffect

}
