package com.adamczewski.kmp.viewmodel

import androidx.lifecycle.ViewModel
import com.adamczewski.kmpmvi.mvi.BaseMviStateManager
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.StateComponent
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.model.NoMessages
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import kotlinx.coroutines.flow.StateFlow

typealias ViewModelContainer<A, S, E> = BaseViewModelContainer<A, S, E, NoMessages>

open class BaseViewModelContainer<Action : MviAction, State : MviState, Effect : MviEffect, Message: MviMessage>(
    private val container: BaseMviStateManager<Action, State, Effect, Message>,
) : ViewModel(AutoCloseable { container.close() }),
    StateComponent<Action, State, Effect> by container

typealias SimpleViewModelContainer<A, S, E> = AbstractViewModelContainer<A, S, E, NoMessages>

abstract class AbstractViewModelContainer<Action : MviAction, State : MviState, Effect : MviEffect, Message: MviMessage> :
    ViewModel(), StateComponent<Action, State, Effect> {

    abstract val container: BaseMviStateManager<Action, State, Effect, Message>

    init {
        addCloseable(AutoCloseable { container.close() })
    }

    override val currentState: StateFlow<State>
        get() = container.currentState

    override val effects: EffectsHandler<Effect>
        get() = container.effects

    override fun submitAction(action: Action) {
        container.submitAction(action)
    }
}
