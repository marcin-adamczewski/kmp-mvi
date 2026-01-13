package com.adamczewski.kmp.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamczewski.kmpmvi.mvi.MviComponent
import com.adamczewski.kmpmvi.mvi.model.MviState

@Composable
public fun <S : MviState> MviComponent<*, S, *>.collectAsStateWithLifecycle(): State<S> =
    lifecycleState.collectAsStateWithLifecycle()
