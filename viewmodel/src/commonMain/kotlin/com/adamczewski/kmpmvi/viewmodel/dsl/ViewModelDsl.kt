package com.adamczewski.kmpmvi.viewmodel.dsl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamczewski.kmpmvi.mvi.BaseMviContainer
import com.adamczewski.kmpmvi.mvi.MviContainer
import com.adamczewski.kmpmvi.mvi.dsl.RootScope
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.settings.MviSettings
import com.adamczewski.kmpmvi.mvi.utils.defaultMviSettings

public fun <Action : MviAction, State : MviState, Effect : MviEffect> ViewModel.viewmodelMvi(
    initialState: State,
    settings: MviSettings? = null,
    logTag: String? = null,
    block: RootScope<Action, State, Effect, Nothing>.() -> Unit
): MviContainer<Action, State, Effect> {
    return this.viewmodelMvi<Action, State, Effect, Nothing>(initialState, settings, logTag, block)
}

public fun <Action : MviAction, State : MviState, Effect : MviEffect, Message : MviMessage> ViewModel.viewmodelMvi(
    initialState: State,
    settings: MviSettings? = null,
    logTag: String? = null,
    block: RootScope<Action, State, Effect, Message>.() -> Unit
): BaseMviContainer<Action, State, Effect, Message> {
    val actualSettings = settings ?: this.defaultMviSettings(logTag = logTag)
    val container = BaseMviContainer<Action, State, Effect, Message>(
        scopeProvider = { viewModelScope },
        initialState = initialState,
        settings = actualSettings
    )
    this.addCloseable(object: AutoCloseable {
        override fun close() {
            container.close()
        }
    })
    RootScope(container).block()
    return container
}
