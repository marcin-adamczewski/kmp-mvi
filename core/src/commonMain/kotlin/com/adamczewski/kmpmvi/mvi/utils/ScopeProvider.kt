package com.adamczewski.kmpmvi.mvi.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.EmptyCoroutineContext

internal object ScopeProvider {
    internal fun createMviScope(): CoroutineScope {
        val dispatcher =
            try {
                // In platforms where `Dispatchers.Main` is not available, Kotlin Multiplatform will
                // throw
                // an exception (the specific exception type may depend on the platform). Since there's
                // no
                // direct functional alternative, we use `EmptyCoroutineContext` to ensure that a
                // coroutine
                // launched within this scope will run in the same context as the caller.
                Dispatchers.Main.immediate
            } catch (_: NotImplementedError) {
                // In Native environments where `Dispatchers.Main` might not exist (e.g., Linux):
                EmptyCoroutineContext
            } catch (_: IllegalStateException) {
                // In JVM Desktop environments where `Dispatchers.Main` might not exist (e.g., Swing):
                EmptyCoroutineContext
            }
        return CoroutineScope(dispatcher + SupervisorJob())
    }
}
