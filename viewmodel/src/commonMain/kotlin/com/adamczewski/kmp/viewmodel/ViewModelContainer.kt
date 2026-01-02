package com.adamczewski.kmp.viewmodel

import androidx.lifecycle.ViewModel
import com.adamczewski.kmpmvi.mvi.BaseMviStateManager
import com.adamczewski.kmpmvi.mvi.model.MviState
import com.adamczewski.kmpmvi.mvi.model.MviEffect
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import kotlinx.coroutines.flow.StateFlow

public typealias ViewModelContainer<A, S, E> = BaseViewModelContainer<A, S, E, Nothing>

public open class BaseViewModelContainer<Action : MviAction, State : MviState, Effect : MviEffect, Message: MviMessage>(
    private val container: BaseMviStateManager<Action, State, Effect, Message>,
) : ViewModel(AutoCloseable { container.close() }),
    MviComponent<Action, State, Effect> by container

public typealias SimpleViewModelContainer<A, S, E> = AbstractViewModelContainer<A, S, E, Nothing>

public abstract class AbstractViewModelContainer<Action : MviAction, State : MviState, Effect : MviEffect, Message: MviMessage> :
    ViewModel(), MviComponent<Action, State, Effect> {

    public abstract val container: BaseMviStateManager<Action, State, Effect, Message>

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
