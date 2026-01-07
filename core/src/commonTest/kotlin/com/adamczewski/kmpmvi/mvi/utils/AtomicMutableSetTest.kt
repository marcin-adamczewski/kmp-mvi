package com.adamczewski.kmpmvi.mvi.utils

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AtomicMutableSetTest {

    @Test
    fun `given empty set when adding all elements then set contains all elements`() = runTest {
        val sut = AtomicMutableSet<Int>()
        sut.addAll(listOf(1, 2))
        assertEquals(mutableSetOf(1, 2), sut)
    }

    @Test
    fun `given empty set when adding an element then set contains this element`() = runTest {
        val sut = AtomicMutableSet<Int>()
        sut.add(1)
        assertEquals(mutableSetOf(1), sut)
    }

    @Test
    fun `given non-empty set when removing some elements then elements removed`() = runTest {
        val sut = AtomicMutableSet<Int>()
        sut.addAll(listOf(1, 2, 3))

        sut.removeAll(listOf(1, 2))

        assertEquals(mutableSetOf(3), sut)
    }

    @Test
    fun `given non-empty set when removing one element then element removed`() = runTest {
        val sut = AtomicMutableSet<Int>()
        sut.addAll(listOf(1, 2, 3))

        sut.remove(2)

        assertEquals(mutableSetOf(1, 3), sut)
    }

    @Test
    fun `given non-empty set when cleared set then all elements removed`() = runTest {
        val sut = AtomicMutableSet<Int>()
        sut.addAll(listOf(1, 2, 3))

        sut.clear()

        assertEquals(mutableSetOf(), sut)
    }

    @Test
    fun `given non-empty set when contains called for existing element then return true`() = runTest {
        val sut = AtomicMutableSet<Int>()
        sut.addAll(listOf(1, 2, 3))

        assertTrue(sut.contains(2))
    }

    @Test
    fun `given non-empty set when contains called for non-existing element then return false`() = runTest {
        val sut = AtomicMutableSet<Int>()
        sut.addAll(listOf(1, 2, 3))

        assertFalse(sut.contains(4))
    }

    @Test
    fun `given empty set when contains called then return false`() = runTest {
        val sut = AtomicMutableSet<Int>()
        assertFalse(sut.contains(4))
    }

    @Test
    fun `given elements added with addAll when elements exceeds max size then remove first elements exceeding the limit`() = runTest {
        val sut = AtomicMutableSet<Int>(maxSize = 2).apply {
            addAll(listOf(1, 2, 3, 4, 5))
        }
        assertEquals(mutableSetOf(4, 5), sut)
    }

    @Test
    fun `given elements added with add when elements exceeds max size then remove first elements exceeding the limit`() = runTest {
        val sut = AtomicMutableSet<Int>(maxSize = 2).apply {
            add(1)
            add(2)
            add(3)
            add(4)
            add(5)
        }
        assertEquals(mutableSetOf(4, 5), sut)
    }

    @Test
    fun `given non-empty set when isEmpty called then return false`() = runTest {
        val sut = AtomicMutableSet<Int>()
        sut.addAll(listOf(1, 2, 3))

        assertFalse(sut.isEmpty())
    }

    @Test
    fun `given empty set when isEmpty called then return true`() = runTest {
        val sut = AtomicMutableSet<Int>()
        assertTrue(sut.isEmpty())
    }
}
