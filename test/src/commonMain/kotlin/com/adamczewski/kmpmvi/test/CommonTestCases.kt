package com.adamczewski.kmpmvi.test

import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.StateComponent
import com.adamczewski.kmpmvi.mvi.error.LONG_ERROR_DURATION
import com.adamczewski.kmpmvi.mvi.error.UiError
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle

public const val TEST_DELAY: Long = 2L

public suspend fun <A : MviAction, E : MviEffect, VM : StateComponent<A, *, E>> TestScope.whenActionThenEffect(
    stateComponent: VM,
    actionToSubmit: A,
    expectedEffect: E,
) {
    whenActionThenEffect(
        stateComponent = stateComponent,
        actionBlock = { submitAction(actionToSubmit) },
        expectedEffect = expectedEffect
    )
}

public suspend fun <A : MviAction, E : MviEffect, VM : StateComponent<A, *, E>> TestScope.whenActionThenEffects(
    stateComponent: VM,
    actionToSubmit: A,
    vararg expectedEffects: E,
) {
    whenActionThenEffects(
        stateComponent = stateComponent,
        actionBlock = { submitAction(actionToSubmit) },
        expectedEffects = expectedEffects
    )
}

public suspend fun <A : MviAction, E : MviEffect, VM : StateComponent<A, *, E>> TestScope.whenActionThenEffect(
    stateComponent: VM,
    actionBlock: suspend StateManagerFlowTurbine<E, A>.() -> Unit,
    expectedEffect: E,
) {
    stateComponent.testEffects(this) {
        expectNoEvents()

        actionBlock()

        assertEquals(expectedEffect, awaitItem())
        expectNoEvents()
    }
}

public suspend fun <A : MviAction, E : MviEffect, VM : StateComponent<A, *, E>> TestScope.whenActionThenEffects(
    stateComponent: VM,
    actionBlock: suspend StateManagerFlowTurbine<E, A>.() -> Unit,
    vararg expectedEffects: E,
) {
    stateComponent.testEffects(this) {
        expectNoEvents()

        actionBlock()

        expectedEffects.forEach { expectedEffect ->
            assertEquals(expectedEffect, awaitItem())
        }
        expectNoEvents()
    }
}

public suspend fun <A : MviAction, E : MviEffect, VM : StateComponent<A, *, E>> TestScope.whenActionThen(
    stateComponent: VM,
    actionToSubmit: A,
    assertBlock: () -> Unit,
) {
    whenActionThen(
        stateComponent = stateComponent,
        actionBlock = { submitAction(actionToSubmit) },
        assertBlock = assertBlock
    )
}

public suspend fun <A : MviAction, E : MviEffect, VM : StateComponent<A, *, E>> TestScope.whenActionThen(
    stateComponent: VM,
    vararg actionToSubmit: A,
    assertBlock: () -> Unit,
) {
    whenActionThen(
        stateComponent = stateComponent,
        actionBlock = { actionToSubmit.forEach { submitAction(it) } },
        assertBlock = assertBlock
    )
}

public suspend fun <A : MviAction, E : MviEffect, VM : StateComponent<A, *, E>> TestScope.whenActionThen(
    stateComponent: VM,
    actionBlock: (StateManagerFlowTurbine<E, A>.() -> Unit),
    assertBlock: () -> Unit,
) {
    stateComponent.testEffects(this) {
        expectNoEvents()

        actionBlock()

        advanceUntilIdle()
        assertBlock()
        cancelAndIgnoreRemainingEvents()
    }
}

public suspend fun <A : MviAction, S, VM : StateComponent<A, S, *>> TestScope.whenTextChangedThenUpdateState(
    stateComponent: VM,
    actionToSubmit: (String) -> A,
    stateFieldToAssert: (S) -> String?,
    initialText: String? = "",
) {
    stateComponent.testState(this) {
        assertEquals(initialText, stateFieldToAssert(expectMostRecentItem()))

        val newText = "changed"
        submitAction(actionToSubmit(newText))

        assertEquals(newText, stateFieldToAssert(expectMostRecentItem()))
    }
}

public suspend fun <A : MviAction, S, VM : StateComponent<A, S, *>> TestScope.whenCheckboxCheckedThenUpdateState(
    stateComponent: VM,
    actionToSubmit: (Boolean) -> A,
    stateFieldToAssert: (S) -> Boolean,
    initialIsChecked: Boolean = false,
) {
    stateComponent.testState(this) {
        assertEquals(initialIsChecked, stateFieldToAssert(expectMostRecentItem()))

        submitAction(actionToSubmit(!initialIsChecked))

        assertEquals(!initialIsChecked, stateFieldToAssert(expectMostRecentItem()))

        submitAction(actionToSubmit(initialIsChecked))

        assertEquals(initialIsChecked, stateFieldToAssert(expectMostRecentItem()))
    }
}

public suspend fun <A : MviAction, S, VM : StateComponent<A, S, *>> TestScope.whenActionThenShowProgress(
    stateComponent: VM,
    stateFieldToAssert: (S) -> Boolean,
    beforeActionBlock: (StateManagerFlowTurbine<S, A>.() -> Unit)? = null,
    actionToSubmit: A? = null,
) {
    stateComponent.testState(this) {
        beforeActionBlock?.invoke(this)
        actionToSubmit?.let { action ->
            assertFalse(stateFieldToAssert(expectMostRecentItem()))
            submitAction(action)
        }
        advanceTimeBy(TEST_DELAY / 2)

        assertTrue(stateFieldToAssert(expectMostRecentItem()))

        advanceTimeBy((TEST_DELAY / 2) + 1)

        assertFalse(stateFieldToAssert(expectMostRecentItem()))
    }
}

public suspend fun <A : MviAction, S, VM : StateComponent<A, S, *>> TestScope.whenActionThenHideProgress(
    stateComponent: VM,
    stateFieldToAssert: (S) -> Boolean,
    actionToSubmit: A,
) {
    stateComponent.testState(this) {
        assertTrue(stateFieldToAssert(expectMostRecentItem()))

        submitAction(actionToSubmit)

        assertFalse(stateFieldToAssert(expectMostRecentItem()))
    }
}

public suspend fun <A : MviAction, S, VM : StateComponent<A, S, *>> TestScope.whenActionThenShowError(
    stateComponent: VM,
    errorFieldProducer: (S) -> UiError?,
    actionToSubmit: A,
    displayTime: Long = LONG_ERROR_DURATION,
    errorTypeAssertion: ((UiError) -> Boolean)? = null,
) {
    whenActionThenShowError(
        stateComponent = stateComponent,
        errorFieldProducer = errorFieldProducer,
        actionBlock = { submitAction(actionToSubmit) },
        displayTime = displayTime,
        errorTypeAssertion = errorTypeAssertion
    )
}

public suspend fun <A : MviAction, S, VM : StateComponent<A, S, *>> TestScope.whenActionThenShowError(
    stateComponent: VM,
    errorFieldProducer: (S) -> UiError?,
    actionToSubmit: A,
    displayTime: Long = LONG_ERROR_DURATION,
    expectedError: UiError,
) {
    whenActionThenShowError(
        stateComponent = stateComponent,
        errorFieldProducer = errorFieldProducer,
        actionBlock = { submitAction(actionToSubmit) },
        displayTime = displayTime,
        errorTypeAssertion = { it == expectedError })
}

public suspend fun <A : MviAction, S, VM : StateComponent<A, S, *>> TestScope.whenInitThenShowError(
    stateComponent: VM,
    errorFieldProducer: (S) -> UiError?,
    fetchDelay: Long = TEST_DELAY,
    displayTime: Long = LONG_ERROR_DURATION,
    errorTypeAssertion: ((UiError) -> Boolean)? = null,
) {
    whenActionThenShowError(
        stateComponent = stateComponent,
        errorFieldProducer = errorFieldProducer,
        actionBlock = { delay(fetchDelay) },
        displayTime = displayTime,
        errorTypeAssertion = errorTypeAssertion
    )
}

public suspend fun <A : MviAction, S, VM : StateComponent<A, S, *>> TestScope.whenActionThenShowError(
    stateComponent: VM,
    errorFieldProducer: (S) -> UiError?,
    actionBlock: suspend StateManagerFlowTurbine<S, A>.() -> Unit,
    displayTime: Long = LONG_ERROR_DURATION,
    errorTypeAssertion: ((UiError) -> Boolean)? = null,
) {
    stateComponent.testState(this) {
        assertNull(errorFieldProducer(expectMostRecentItem()))

        this.actionBlock()

        advanceTimeBy(displayTime / 2)
        errorFieldProducer(expectMostRecentItem()).run {
            assertNotNull(this)
            assertTrue(errorTypeAssertion?.invoke(this) ?: true)
        }

        advanceTimeBy((displayTime / 2) + 1)
        assertNull(errorFieldProducer(expectMostRecentItem()))
    }
}

public suspend fun <T, A : MviAction, S, VM : StateComponent<A, S, *>> TestScope.testItemToggled(
    stateComponent: VM,
    itemsCollection: (S) -> Set<T>,
    toggledItem: T,
    action: (T) -> A,
) {
    stateComponent.testState(this) {
        assertTrue(itemsCollection(expectMostRecentItem()).isEmpty())

        submitAction(action(toggledItem))

        assertEquals(
            setOf(toggledItem), itemsCollection(expectMostRecentItem())
        )

        submitAction(action(toggledItem))

        assertTrue(itemsCollection(expectMostRecentItem()).isEmpty())
    }
}
