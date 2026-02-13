package com.adamczewski.kmpmvi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.model.MviEffect

@Composable
public fun <E : MviEffect> MviComponent<*, *, E>.ConsumeEffects(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    handler: suspend (E) -> Unit,
) {
    effects.ConsumeEffects(
        handler = handler,
        lifecycleOwner = lifecycleOwner,
        minActiveState = minActiveState
    )
}

@Composable
public fun <E : MviEffect> EffectsHandler<E>.ConsumeEffects(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    handler: suspend (E) -> Unit,
) {
    LaunchedEffect(this, lifecycleOwner, minActiveState) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            consume(handler = handler)
        }
    }
}
