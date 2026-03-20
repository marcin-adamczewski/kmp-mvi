package com.adamczewski.kmpmvi.mvi.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.skip

public class CombinedProgressPublisher(
    vararg progressObservables: ProgressObservable,
) : ProgressObservable {
    override val isLoading: Flow<Boolean> =
        combine(
            progressObservables
                .map {
                    it.isLoading.onStart { emit(false) }
                }
        ) { progresses ->
            progresses.any { isLoading -> isLoading }
        }.drop(1)
}
