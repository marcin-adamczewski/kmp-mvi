package com.adamczewski.kmpmvi.mvi.android

import androidx.lifecycle.ViewModel
import com.adamczewski.kmpmvi.mvi.MVIState
import com.adamczewski.kmpmvi.mvi.MviEffect
import com.adamczewski.kmpmvi.mvi.MviStateManager
import com.adamczewski.kmpmvi.mvi.StateComponent
import com.adamczewski.kmpmvi.mvi.MviAction
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import kotlinx.coroutines.flow.StateFlow

open class ViewModelContainer<Action : MviAction, State : MVIState, Effect : MviEffect>(
    private val container: MviStateManager<Action, State, Effect>,
) : ViewModel(AutoCloseable { container.close() }),
    StateComponent<Action, State, Effect> by container

abstract class BaseViewModelContainer<Action : MviAction, State : MVIState, Effect : MviEffect> :
    ViewModel(), StateComponent<Action, State, Effect> {

    abstract val container: MviStateManager<Action, State, Effect>

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
