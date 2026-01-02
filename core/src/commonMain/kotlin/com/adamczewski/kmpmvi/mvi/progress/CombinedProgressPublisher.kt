package com.adamczewski.kmpmvi.mvi.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

public class CombinedProgressPublisher(
    vararg progressObservables: ProgressObservable,
) : ProgressObservable {
    override val isLoading: Flow<Boolean> =
        combine(progressObservables.map { it.isLoading }) { progresses ->
            progresses.any { isInProgress -> isInProgress }
        }
}
