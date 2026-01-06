package kmpmvi.state

import app.cash.turbine.test
import com.adamczewski.kmpmvi.mvi.state.SubscriberCountStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriberCountStateFlowTest {

    @Test
    fun `given SubscriberCountStateFlow, when created, then subscription count is 0`() = runTest {
        val upstream = MutableStateFlow(1)
        val sut = SubscriberCountStateFlow(upstream)

        sut.subscriptionCount.test {
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun `given SubscriberCountStateFlow, when collected, then subscription count increases and decreases on cancel`() = runTest {
        val upstream = MutableStateFlow(1)
        val sut = SubscriberCountStateFlow(upstream)

        sut.subscriptionCount.test {
            assertEquals(0, awaitItem())

            val job = launch {
                sut.collect { }
            }

            assertEquals(1, awaitItem())

            job.cancelAndJoin()
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun `when multiple collectors, then subscription count increases accordingly`() = runTest {
        val upstream = MutableStateFlow(1)
        val sut = SubscriberCountStateFlow(upstream)

        sut.subscriptionCount.test {
            assertEquals(0, awaitItem())

            val job1 = launch { sut.collect { } }
            assertEquals(1, awaitItem())

            val job2 = launch { sut.collect { } }
            assertEquals(2, awaitItem())

            job1.cancelAndJoin()
            assertEquals(1, awaitItem())

            job2.cancelAndJoin()
            assertEquals(0, awaitItem())
        }
    }

    @Test
    fun `given SubscriberCountStateFlow, then value and replay cache are delegated to upstream`() {
        val upstream = MutableStateFlow(1)
        val sut = SubscriberCountStateFlow(upstream)

        assertEquals(1, sut.value)
        assertEquals(listOf(1), sut.replayCache)

        upstream.value = 2

        assertEquals(2, sut.value)
        assertEquals(listOf(2), sut.replayCache)
    }

    @Test
    fun `when upstream emits, then collector receives values`() = runTest {
        val upstream = MutableStateFlow(1)
        val sut = SubscriberCountStateFlow(upstream)

        sut.test {
            assertEquals(1, awaitItem())

            upstream.value = 2

            assertEquals(2, awaitItem())

            upstream.value = 3

            assertEquals(3, awaitItem())
        }
    }

    @Test
    fun `when upstream emits same values twice, then collector receives value once`() = runTest {
        val upstream = MutableStateFlow(1)
        val sut = SubscriberCountStateFlow(upstream)

        sut.test {
            assertEquals(1, awaitItem())

            upstream.update { 2 }
            upstream.update { 2 }

            assertEquals(2, awaitItem())
            expectNoEvents()
        }
    }
}