package com.adamczewski.kmpmvi.test

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.StateComponent
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

class StateManagerFlowTurbine<T, A : MviAction>(
    private val testFlow: ReceiveTurbine<T>,
    private val stateComponent: StateComponent<A, *, *>,
    private val testScope: TestScope,
) : ReceiveTurbine<T> by testFlow {

    fun submitAction(action: A) {
        stateComponent.submitAction(action)
    }

    fun assertNoEvents() {
        testScope.advanceUntilIdle()
        testFlow.expectNoEvents()
    }
}

suspend fun <E : MviEffect, A : MviAction> StateComponent<A, *, E>.testEffects(
    scope: TestScope,
    validate: suspend StateManagerFlowTurbine<E, A>.() -> Unit,
) {
    effects.consumeFlow(handler = {}).test {
        StateManagerFlowTurbine(this, this@testEffects, scope).validate()
    }
}

suspend fun <S, A : MviAction> StateComponent<A, S, *>.testState(
    scope: TestScope,
    validate: suspend StateManagerFlowTurbine<S, A>.() -> Unit,
) {
    currentState.test {
        StateManagerFlowTurbine(this, this@testState, scope).validate()
    }
}
