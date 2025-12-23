package com.adamczewski.kmpmvi.mvi.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CombinedProgressPublisher(
    vararg progressObservables: ProgressObservable,
) : ProgressObservable {
    override val observeState: Flow<Boolean> =
        combine(progressObservables.map { it.observeState }) { progresses ->
            progresses.any { isInProgress -> isInProgress }
        }
}
