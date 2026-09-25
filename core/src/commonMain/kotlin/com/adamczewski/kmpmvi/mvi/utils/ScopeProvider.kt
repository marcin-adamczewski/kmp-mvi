package com.adamczewski.kmpmvi.mvi.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal object ScopeProvider {
    internal fun createMviScope(): CoroutineScope {
        val dispatcher =
            try {
                Dispatchers.Main.immediate
            } catch (e: UnsupportedOperationException) {
                // Main exists, but doesn't support immediate execution.
                // Fall back to standard Main to stay on the UI thread.
                Dispatchers.Main
            } catch (_: NotImplementedError) {
                // In Native environments where `Dispatchers.Main` might not exist (e.g., Linux):
                fallbackDispatcher()
            } catch (_: IllegalStateException) {
                // In JVM Desktop environments where `Dispatchers.Main` might not exist (e.g., Swing):
                fallbackDispatcher()
            }
        return CoroutineScope(dispatcher + SupervisorJob())
    }

    private fun fallbackDispatcher(): CoroutineDispatcher =
        Dispatchers.Default.limitedParallelism(1)
}
