package com.adamczewski.kmpmvi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.effects.EffectsHandler
import com.adamczewski.kmpmvi.mvi.model.MviEffect

@Composable
public fun <E: MviEffect> MviComponent<*, *, E>.ConsumeEffects(
    handler: suspend (E) -> Unit,
) {
    effects.ConsumeEffects(handler = handler)
}

@Composable
public fun <E: MviEffect> EffectsHandler<E>.ConsumeEffects(
    handler: suspend (E) -> Unit,
) {
    LaunchedEffect(this) {
        consume(handler = handler)
    }
}
