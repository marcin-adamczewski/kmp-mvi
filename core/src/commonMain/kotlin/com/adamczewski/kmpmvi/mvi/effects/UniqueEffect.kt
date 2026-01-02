@file:OptIn(ExperimentalUuidApi::class)

package com.adamczewski.kmpmvi.mvi.effects

import com.adamczewski.kmpmvi.mvi.model.MviEffect
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

public class UniqueEffect<T : MviEffect>(
    public val effect: T,
    public val id: String = Uuid.random().toString(),
)
