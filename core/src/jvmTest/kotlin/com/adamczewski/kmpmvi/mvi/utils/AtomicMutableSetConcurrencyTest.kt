package com.adamczewski.kmpmvi.mvi.utils

import java.util.Collections
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicIntegerArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises [AtomicMutableSet] under real thread contention. These are JVM-only because they need
 * actual parallelism. They assert the exactly-once return contract of add/remove that the effect
 * dedup relies on: a correct CAS-based implementation always satisfies it (so these never flake on
 * correct code), while a non-atomic implementation would report an element added/removed more than
 * once.
 */
class AtomicMutableSetConcurrencyTest {

    @Test
    fun `given many threads adding the same elements then add returns true exactly once per element`() {
        repeat(REPEATS) {
            val sut = AtomicMutableSet<Int>()
            val trueCounts = AtomicIntegerArray(ELEMENT_COUNT)

            runContended { _ ->
                for (element in 0 until ELEMENT_COUNT) {
                    if (sut.add(element)) trueCounts.incrementAndGet(element)
                }
            }

            for (element in 0 until ELEMENT_COUNT) {
                assertEquals(1, trueCounts.get(element), "element $element was added more than once")
            }
            assertEquals((0 until ELEMENT_COUNT).toSet(), sut.toSet())
        }
    }

    @Test
    fun `given many threads removing the same elements then remove returns true exactly once per element`() {
        repeat(REPEATS) {
            val sut = AtomicMutableSet<Int>()
            (0 until ELEMENT_COUNT).forEach { sut.add(it) }
            val trueCounts = AtomicIntegerArray(ELEMENT_COUNT)

            runContended { _ ->
                for (element in 0 until ELEMENT_COUNT) {
                    if (sut.remove(element)) trueCounts.incrementAndGet(element)
                }
            }

            for (element in 0 until ELEMENT_COUNT) {
                assertEquals(1, trueCounts.get(element), "element $element was removed more than once")
            }
            assertTrue(sut.toSet().isEmpty())
        }
    }

    @Test
    fun `given many threads adding and removing distinct elements then final set is exactly the survivors`() {
        repeat(REPEATS) {
            val sut = AtomicMutableSet<Int>()

            // Each thread owns a disjoint range: it adds every element, then removes the first half.
            // The survivors are the second half of every thread's range, so the outcome is fully
            // deterministic despite running concurrently.
            runContended { threadIndex ->
                val base = threadIndex * ELEMENT_COUNT
                for (offset in 0 until ELEMENT_COUNT) sut.add(base + offset)
                for (offset in 0 until ELEMENT_COUNT / 2) sut.remove(base + offset)
            }

            val expected = (0 until THREAD_COUNT).flatMap { threadIndex ->
                val base = threadIndex * ELEMENT_COUNT
                (ELEMENT_COUNT / 2 until ELEMENT_COUNT).map { base + it }
            }.toSet()
            assertEquals(expected, sut.toSet())
        }
    }

    private fun runContended(work: (threadIndex: Int) -> Unit) {
        val barrier = CyclicBarrier(THREAD_COUNT)
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        val threads = (0 until THREAD_COUNT).map { threadIndex ->
            Thread {
                try {
                    barrier.await()
                    work(threadIndex)
                } catch (t: Throwable) {
                    failures.add(t)
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertTrue(failures.isEmpty(), "threads failed: $failures")
    }

    private companion object {
        private const val THREAD_COUNT = 8
        private const val ELEMENT_COUNT = 200
        private const val REPEATS = 50
    }
}
