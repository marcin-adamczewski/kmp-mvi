package com.adamczewski.kmpmvi.mvi.model

interface MviState {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

data object NoState : MviState
