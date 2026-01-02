package com.adamczewski.kmpmvi.mvi.progress

import kotlinx.coroutines.flow.Flow

public interface ProgressObservable {
    public val isLoading: Flow<Boolean>
}
