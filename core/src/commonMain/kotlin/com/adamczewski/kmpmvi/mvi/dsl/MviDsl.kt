package com.adamczewski.kmpmvi.mvi.dsl

import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.MviContainer
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.settings.DefaultMviSettingsProvider
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import com.adamczewski.kmpmvi.mvi.utils.defaultMviSettings
import kotlinx.coroutines.CoroutineScope
import kotlin.jvm.JvmName

@DslMarker
public annotation class MviDsl

public fun <Container : Any, Action : MviAction, State : MviState, Effect : MviEffect> Container.mvi(
    initialState: State,
    scope: CoroutineScope? = null,
    settings: MviSettings? = null,
    logTag: String? = null,
    block: RootScope<Action, State, Effect, Nothing>.() -> Unit
): MviContainer<Action, State, Effect> {
    return this.mvi<Container, Action, State, Effect, Nothing>(
        initialState = initialState,
        scope = scope,
        settings = settings,
        logTag = logTag,
        block = block
    )
}

@JvmName("mviWithMessage")
public fun <Container : Any, Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage> Container.mvi(
    initialState: State,
    scope: CoroutineScope? = null,
    settings: MviSettings? = null,
    logTag: String? = null,
    block: RootScope<Action, State, Effect, Message>.() -> Unit
): BaseMviContainer<Action, State, Effect, Message> {
    val actualSettings = settings ?: this.defaultMviSettings(logTag = logTag)
    val container = BaseMviContainer<Action, State, Effect, Message>(
        scopeProvider = { scope ?: actualSettings.scopeProvider() },
        initialState = initialState,
        settings = actualSettings
    )
    RootScope(container).block()
    return container
}
