package kmpmvi

import com.adamczewski.kmpmvi.mvi.BaseMviStateManager
import com.adamczewski.kmpmvi.mvi.model.MviAction
import com.adamczewski.kmpmvi.mvi.model.MviMessage
import com.adamczewski.kmpmvi.mvi.MviStateManager
import com.adamczewski.kmpmvi.mvi.model.NoActions
import com.adamczewski.kmpmvi.mvi.model.NoEffects
import com.adamczewski.kmpmvi.mvi.model.NoState
import com.adamczewski.kmpmvi.mvi.Settings
import com.adamczewski.kmpmvi.mvi.actions.ActionsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MessagingStateManagerTest {

    private val scopeProvider = { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    @BeforeEach
    fun beforeEach() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given observer using onMessage, when message emitted, then observer notified`() = runTest {
        val childStateManager = TestMessagingStateManager()
        var receivedMessage: String? = null
        val parentStateManager =
            object : MviStateManager<NoActions, NoState, NoEffects>(
                settings = Settings(scopeProvider = scopeProvider),
                initialState = NoState
            ) {
                init {
                    onMessage<TestMessagingMessage.NewMessagingMessage>(childStateManager) { message ->
                        receivedMessage = message.message
                    }
                }

                override fun ActionsManager<NoActions>.handleActions() {}
            }

        childStateManager.submitAction(TestMessagingAction.ProcessMessage(TEST_MESSAGE))

        assertEquals(TEST_MESSAGE, receivedMessage)
        childStateManager.close()
        parentStateManager.close()
    }

    @Test
    fun `given observer using onMessageFlow, when message emitted, then observer notified`() =
        runTest {
            val childStateManager = TestMessagingStateManager()
            var receivedMessage: String? = null
            val parentStateManager =
                object : MviStateManager<NoActions, NoState, NoEffects>(
                    initialState = NoState
                ) {
                    init {
                        onMessageFlow<TestMessagingMessage.NewMessagingMessage>(childStateManager) {
                            onEach {
                                receivedMessage = it.message
                            }
                        }
                    }

                    override fun ActionsManager<NoActions>.handleActions() {}
                }

            childStateManager.submitAction(TestMessagingAction.ProcessMessage(TEST_MESSAGE))

            assertEquals(TEST_MESSAGE, receivedMessage)
            childStateManager.close()
            parentStateManager.close()
        }

    companion object {
        private const val TEST_MESSAGE = "Test Message"
    }
}

private sealed interface TestMessagingAction : MviAction {
    data class ProcessMessage(val message: String) : TestMessagingAction
}

private sealed interface TestMessagingMessage : MviMessage {
    data class NewMessagingMessage(val message: String) : TestMessagingMessage
}

private class TestMessagingStateManager :
    BaseMviStateManager<TestMessagingAction, NoState, NoEffects, TestMessagingMessage>(
        initialState = NoState,
        settings = Settings(scopeProvider = { CoroutineScope(SupervisorJob() + Dispatchers.Main) })
    ) {
    override fun ActionsManager<TestMessagingAction>.handleActions() {
        onAction<TestMessagingAction.ProcessMessage> { action ->
            setMessage { TestMessagingMessage.NewMessagingMessage(action.message) }
        }
    }
}
