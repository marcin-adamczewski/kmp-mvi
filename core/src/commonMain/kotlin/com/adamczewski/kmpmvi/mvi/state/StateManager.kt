package com.adamczewski.kmpmvi.mvi.state

import com.adamczewski.kmpmvi.mvi.model.MviState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class StateManager<State: MviState>(initialState: State) {
    private val _state = MutableStateFlow(initialState)
    internal val state: StateFlow<State> = _state.asStateFlow()

    private val _subscriberCountState: SubscriberCountStateFlow<State> = SubscriberCountStateFlow(_state)
    internal val subscriberCountState: StateFlow<State> = _subscriberCountState

    internal val subscribersCount: StateFlow<Int> = _subscriberCountState.subscriptionCount

    internal val stateValue: State get() = state.value

    internal fun setState(reducer: State.() -> State) {
        _state.update { currentValue -> reducer(currentValue) }
    }
}
