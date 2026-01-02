/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adamczewski.kmpmvi.mvi.error

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow

public typealias ErrorManager = BaseErrorManager<UiError>

public class BaseErrorManager<E: MviError> {
    // Maximum of 3 errors are queued
    private val pendingErrors = Channel<E>(3, BufferOverflow.DROP_OLDEST)
    private val removeErrorSignal = Channel<Unit>(Channel.RENDEZVOUS)

    /**
     * A flow of [MviError]s to display in the UI, usually as snackbars. The flow will immediately
     * emit `null`, and will then emit errors sent via [addError]. Once duration seconds has elapsed,
     * or [removeCurrentError] is called (if before that) `null` will be emitted to remove
     * the current error.
     */
    public val errors: Flow<E?> = flow {
        emit(null)

        pendingErrors.receiveAsFlow().collect { error ->
            emit(error)

            // Wait for either a durationMs timeout, or a remove signal (whichever comes first)
            merge(
                flow {
                    delay(error.durationMs)
                    emit(Unit)
                },
                removeErrorSignal.receiveAsFlow()
            ).firstOrNull()

            // Remove the error
            emit(null)
        }
    }

    public suspend fun addError(error: E) {
        pendingErrors.send(error)
    }

    public suspend fun removeCurrentError() {
        removeErrorSignal.send(Unit)
    }
}
