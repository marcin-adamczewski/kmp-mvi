package com.adamczewski.kmpmvi.mvi.model

public interface MviState {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

public data object NoState : MviState
