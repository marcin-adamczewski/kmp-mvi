package com.adamczewski.kmpmvi.mvi.utils

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update

/**
 * A multiplatform, thread-safe [MutableSet], implemented using AtomicFU.
 */
internal class AtomicMutableSet<V>(
    private val maxSize: Int = Int.MAX_VALUE
) : MutableSet<V> {
    private val set = AtomicReference(setOf<V>())

    override val size: Int
        get() = set.load().size

    override fun iterator(): MutableIterator<V> {
        return set.load().toMutableList().iterator()
    }

    override fun add(element: V): Boolean {
        var updated = false
        set.update { current ->
            val newSet = buildSet {
                addAll(current)
                updated = add(element)
            }
            newSet.removeFirstExceedingSizeItems()
        }
        return updated
    }

    override fun remove(element: V): Boolean {
        var updated = false
        set.update { current ->
            buildSet {
                addAll(current)
                updated = remove(element)
            }
        }
        return updated
    }

    override fun addAll(elements: Collection<V>): Boolean {
        var updated = false
        set.update { current ->
            val newSet = buildSet {
                addAll(current)
                updated = addAll(elements)
            }
            newSet.removeFirstExceedingSizeItems()
        }
        return updated
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        var updated = false
        set.update { current ->
            buildSet {
                addAll(current)
                updated = removeAll(elements)
            }
        }
        return updated
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        var updated = false
        set.update { current ->
            val newSet = current.toMutableSet()
            updated = newSet.retainAll(elements)
            newSet
        }
        return updated
    }

    override fun clear() {
        set.update { mutableSetOf() }
    }

    override fun isEmpty(): Boolean = set.load().isEmpty()

    override fun contains(element: V): Boolean {
        return set.load().contains(element)
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return set.load().containsAll(elements)
    }

    private fun Set<V>.removeFirstExceedingSizeItems() : Set<V> {
        val exceededMaxSize = (size - maxSize).coerceAtLeast(0)
        return toMutableSet().apply {
            val iterator = iterator()
            repeat(exceededMaxSize) {
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return set.load().equals(other)
    }

    override fun hashCode(): Int {
        return set.load().hashCode()
    }

    override fun toString(): String {
        return set.load().toString()
    }
}
