package com.adamczewski.kmpmvi.mvi.progress

import kotlinx.coroutines.flow.Flow

interface ProgressObservable {
    val isLoading: Flow<Boolean>
}
