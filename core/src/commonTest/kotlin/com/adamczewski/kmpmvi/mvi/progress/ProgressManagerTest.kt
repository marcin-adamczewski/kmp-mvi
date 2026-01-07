package com.adamczewski.kmpmvi.mvi.progress

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressManagerTest {

    private fun createSut(): ProgressManager {
        return ProgressManager()
    }

    @Test
    fun `AddRemoveProgress - filter out initial value`() = runTest {
        createSut().isLoading.test {
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `AddRemoveProgress - when progress added then show loading`() = runTest {
        val sut = createSut()
        sut.isLoading.test {
            expectNoEvents()

            sut.addProgress("a")

            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `AddRemoveProgress - given progress with id when progress with the same id added then no changes in progress`() =
        runTest {
            val sut = createSut()
            sut.isLoading.test {
                sut.addProgress("a")
                assertTrue(awaitItem())

                sut.addProgress("a")

                expectNoEvents()
            }
        }

    @Test
    fun `AddRemoveProgress - given progress with id when progress with different id removed then no changes in progress`() =
        runTest {
            val sut = createSut()
            sut.isLoading.test {
                sut.addProgress("a")
                assertTrue(awaitItem())

                sut.removeProgress("unknown")

                expectNoEvents()
            }
        }

    @Test
    fun `AddRemoveProgress - when two progresses with different id added then hide progress when both are removed`() =
        runTest {
            val sut = createSut()
            sut.isLoading.test {
                sut.addProgress("a")
                assertTrue(awaitItem())
                sut.addProgress("b")
                expectNoEvents()

                sut.removeProgress("a")
                expectNoEvents()
                sut.removeProgress("b")
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `WatchProgress - when shared flow with watch progress subscribed then show loading and hide when first item emitted`() =
        runTest {
            val sut = createSut()
            val source = MutableSharedFlow<Int>()
            sut.isLoading.test {
                val isLoading: ReceiveTurbine<Boolean> = this
                expectNoEvents()

                source.watchProgress(sut).test {
                    assertTrue(isLoading.awaitItem())

                    source.emit(1)
                    assertFalse(isLoading.awaitItem())

                    source.emit(2)
                    isLoading.expectNoEvents()

                    assertEquals(1, awaitItem())
                    assertEquals(2, awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }

                expectNoEvents()
                cancel()
            }
        }

    @Test
    fun `WatchProgress - when flow with watch progress subscribed then show loading and hide when flow completed`() =
        runTest {
            val sut = createSut()
            val source = flow { emit(1) }
            sut.isLoading.test {
                val isLoading: ReceiveTurbine<Boolean> = this
                expectNoEvents()

                source.watchProgress(sut).test {
                    assertTrue(isLoading.awaitItem())
                    assertFalse(isLoading.awaitItem())
                    cancelAndIgnoreRemainingEvents()
                }

                expectNoEvents()
                cancel()
            }
        }

    @Test
    fun `WatchProgress - when flow with watch progress subscribed then show loading and hide when error`() =
        runTest {
            val sut = createSut()
            val source = flow<Any> { throw IllegalStateException() }
            sut.isLoading.test {
                val isLoading: ReceiveTurbine<Boolean> = this
                expectNoEvents()

                source.watchProgress(sut).test {
                    assertTrue(isLoading.awaitItem())
                    assertFalse(isLoading.awaitItem())
                    awaitError()
                    cancel()
                }

                expectNoEvents()
                cancel()
            }
        }

    @Test
    fun `WithProgress - when with progress called then show loading and hide when block completed`() =
        runTest {
            val sut = createSut()
            sut.isLoading.test {
                expectNoEvents()

                withProgress(sut) {
                    assertTrue(awaitItem())
                }
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `WithProgress - when with progress called with error in body then show loading and hide when block completed`() =
        runTest {
            val sut = createSut()
            runCatching {
                sut.isLoading
                    .test {
                        expectNoEvents()
                        try {
                            withProgress(sut) {
                                assertTrue(awaitItem())
                                throw IllegalStateException()
                            }
                        } finally {
                            assertFalse(awaitItem())
                        }
                    }
            }
        }
}
