package com.adamczewski.kmpmvi.mvi.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ScopeProviderTest {

    @BeforeTest
    fun setUp() {
        // Ensure no test Main dispatcher is installed so createMviScope exercises the fallback
        // dispatcher (plain JVM has no Dispatchers.Main).
        Dispatchers.resetMain()
    }

    @Test
    fun `given no Main dispatcher when many coroutines launched then they run serially in order`() {
        val scope = ScopeProvider.createMviScope()
        val results = mutableListOf<Int>()
        val count = 1000

        runBlocking {
            (0 until count)
                .map { i -> scope.launch { results.add(i) } }
                .joinAll()
        }

        assertEquals((0 until count).toList(), results)
    }
}
