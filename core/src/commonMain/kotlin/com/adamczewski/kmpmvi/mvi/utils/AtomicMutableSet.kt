package com.adamczewski.kmpmvi.mvi.utils

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update

/**
 * A small multiplatform, thread-safe set backed by an immutable snapshot held in an
 * [AtomicReference]. Every mutation replaces the snapshot with a new one via an atomic
 * compare-and-set, and reads observe a consistent snapshot.
 *
 * It deliberately does not implement [MutableSet]: a lock-free snapshot can't honour a mutating
 * [MutableIterator] correctly, so rather than exposing a broken iterator this type offers only the
 * operations the library needs, plus [toSet] for a snapshot.
 *
 * When [maxSize] is exceeded, the oldest entries (by insertion order) are evicted.
 */
@PublishedApi
internal class AtomicMutableSet<V>(
    private val maxSize: Int = Int.MAX_VALUE
) {
    private val set = AtomicReference(setOf<V>())

    val size: Int get() = set.load().size

    fun isEmpty(): Boolean = set.load().isEmpty()

    fun contains(element: V): Boolean = set.load().contains(element)

    /** Adds [element], returning true if it was not already present. */
    fun add(element: V): Boolean {
        var added = false
        set.update { current ->
            if (element in current) {
                added = false
                current
            } else {
                added = true
                (current + element).evictExceedingMaxSize()
            }
        }
        return added
    }

    /** Adds all [elements], returning true if the set changed. */
    fun addAll(elements: Collection<V>): Boolean {
        var changed = false
        set.update { current ->
            val next = current + elements
            changed = next.size != current.size
            next.evictExceedingMaxSize()
        }
        return changed
    }

    /** Removes [element], returning true if it was present. */
    fun remove(element: V): Boolean {
        var removed = false
        set.update { current ->
            if (element in current) {
                removed = true
                current - element
            } else {
                removed = false
                current
            }
        }
        return removed
    }

    /** Removes all [elements], returning true if the set changed. */
    fun removeAll(elements: Collection<V>): Boolean {
        var changed = false
        val toRemove = elements.toSet()
        set.update { current ->
            val next = current - toRemove
            changed = next.size != current.size
            next
        }
        return changed
    }

    fun clear() {
        set.store(setOf())
    }

    /** Returns an immutable snapshot of the current elements. */
    fun toSet(): Set<V> = set.load()

    private fun Set<V>.evictExceedingMaxSize(): Set<V> {
        val excess = size - maxSize
        return if (excess <= 0) this else drop(excess).toSet()
    }
}
