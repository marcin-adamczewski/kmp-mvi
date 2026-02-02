package com.adamczewski.kmpmvi.mvi.dsl

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.BaseMviStateManager
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.MviContainer
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.settings.DefaultMviSettingsProvider
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import kotlinx.coroutines.CoroutineScope

@DslMarker
public annotation class MviDsl

public fun <Action : MviAction, State : MviState, Effect : MviEffect> mvi(
    initialState: State,
    scope: CoroutineScope? = null,
    settings: MviSettings? = null,
    block: MviDslBuilder<Action, State, Effect, Nothing>.() -> Unit
): MviContainer<Action, State, Effect> {
    val actualSettings =
        settings ?: DefaultMviSettingsProvider.provide("mvi-dsl", MviComponent::class)
    val container = BaseMviContainer<Action, State, Effect, Nothing>(
        scopeProvider = { scope ?: actualSettings.scopeProvider() },
        initialState = initialState,
        settings = actualSettings
    )
    MviDslBuilder(container).apply(block)
    return container
}

public fun <Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage>
        BaseMviContainer<Action, State, Effect, Message>.mvi(
    block: MviDslBuilder<Action, State, Effect, Message>.() -> Unit
) {
    MviDslBuilder(this).apply(block)
}

public fun <Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage>
        BaseMviStateManager<Action, State, Effect, Message>.mvi(
    block: MviDslBuilder<Action, State, Effect, Message>.() -> Unit
) {
    MviDslBuilder(container).apply(block)
}
