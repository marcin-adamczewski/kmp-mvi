package com.adamczewski.kmpmvi.mvi.utils

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.update

/**
 * A multiplatform, thread-safe [MutableSet], implemented using AtomicFU.
 */
internal class AtomicMutableSet<V>() : MutableSet<V> {
    private val map = AtomicReference(setOf<V>())

    override val size: Int
        get() = map.load().size

    override fun iterator(): MutableIterator<V> {
        return map.load().toMutableList().iterator()
    }

    override fun add(element: V): Boolean {
        var updated = false
        map.update { current ->
            buildSet {
                addAll(current)
                updated = add(element)
            }
        }
        return updated
    }

    override fun remove(element: V): Boolean {
        var updated = false
        map.update { current ->
            buildSet {
                addAll(current)
                updated = remove(element)
            }
        }
        return updated
    }

    override fun addAll(elements: Collection<V>): Boolean {
        var updated = false
        map.update { current ->
            buildSet {
                addAll(current)
                updated = addAll(elements)
            }
        }
        return updated
    }

    override fun removeAll(elements: Collection<V>): Boolean {
        var updated = false
        map.update { current ->
            buildSet {
                addAll(current)
                updated = removeAll(elements)
            }
        }
        return updated
    }

    override fun retainAll(elements: Collection<V>): Boolean {
        var updated = false
        map.update { current ->
            val newSet = current.toMutableSet()
            updated = newSet.retainAll(elements)
            newSet
        }
        return updated
    }

    override fun clear() {
        map.update { mutableSetOf() }
    }

    override fun isEmpty(): Boolean = map.load().isEmpty()

    override fun contains(element: V): Boolean {
        return map.load().contains(element)
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        return map.load().containsAll(elements)
    }
}