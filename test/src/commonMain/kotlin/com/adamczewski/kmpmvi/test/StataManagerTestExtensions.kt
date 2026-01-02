package com.adamczewski.kmpmvi.test

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.MviComponent
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

public class StateManagerFlowTurbine<T, A : MviAction>(
    private val testFlow: ReceiveTurbine<T>,
    private val mviComponent: MviComponent<A, *, *>,
    private val testScope: TestScope,
) : ReceiveTurbine<T> by testFlow {

    public fun submitAction(action: A) {
        mviComponent.submitAction(action)
    }

    public fun assertNoEvents() {
        testScope.advanceUntilIdle()
        testFlow.expectNoEvents()
    }
}

public suspend fun <E : MviEffect, A : MviAction> MviComponent<A, *, E>.testEffects(
    scope: TestScope,
    validate: suspend StateManagerFlowTurbine<E, A>.() -> Unit,
) {
    effects.consumeFlow(handler = {}).test {
        StateManagerFlowTurbine(this, this@testEffects, scope).validate()
    }
}

public suspend fun <S, A : MviAction> MviComponent<A, S, *>.testState(
    scope: TestScope,
    validate: suspend StateManagerFlowTurbine<S, A>.() -> Unit,
) {
    currentState.test {
        StateManagerFlowTurbine(this, this@testState, scope).validate()
    }
}
