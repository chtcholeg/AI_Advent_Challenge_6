package ru.chtcholeg.app.presentation.chat

import ru.chtcholeg.app.data.repository.ChatRepository
import ru.chtcholeg.app.data.repository.SettingsRepository
import ru.chtcholeg.app.domain.model.ChatMessage
import ru.chtcholeg.app.domain.model.MessageType
import ru.chtcholeg.app.domain.model.ResponseMode
import ru.chtcholeg.app.domain.usecase.SendMessageUseCase
import ru.chtcholeg.app.util.ClipboardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatStore(
    private val sendMessageUseCase: SendMessageUseCase,
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val coroutineScope: CoroutineScope
) {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var lastUserMessage: String? = null
    private var currentResponseMode: ResponseMode = ResponseMode.NORMAL

    init {
        // Watch for settings changes
        coroutineScope.launch {
            settingsRepository.settings.collect { settings ->
                handleResponseModeChange(settings.responseMode)
            }
        }
    }

    private fun handleResponseModeChange(newMode: ResponseMode) {
        if (newMode != currentResponseMode) {
            currentResponseMode = newMode
            val settings = settingsRepository.settings.value
            val preserveHistory = settings.preserveHistoryOnSystemPromptChange

            // Determine system message text based on mode
            val systemMessageText = when (newMode) {
                ResponseMode.STRUCTURED_JSON -> "Structured JSON response mode enabled. All AI responses will be in JSON format."
                ResponseMode.STRUCTURED_XML -> "Structured XML response mode enabled. All AI responses will be in XML format."
                ResponseMode.DIALOG -> "Dialog mode enabled. AI will ask clarifying questions one at a time to gather all necessary information before providing final result."
                ResponseMode.STEP_BY_STEP -> "Step-by-Step reasoning mode enabled. AI will solve problems by breaking them down into clear, logical steps."
                ResponseMode.EXPERT_PANEL -> "Expert Panel mode enabled. AI will simulate a panel of experts discussing the topic from different perspectives."
                ResponseMode.NORMAL -> null
            }

            if (systemMessageText != null) {
                val systemMessage = ChatMessage(
                    content = systemMessageText,
                    isFromUser = false,
                    messageType = MessageType.SYSTEM
                )

                if (preserveHistory) {
                    // Keep existing messages, just update/add system message
                    _state.update {
                        it.copy(messages = it.messages.filter { msg -> msg.messageType != MessageType.SYSTEM } + systemMessage)
                    }
                } else {
                    // Clear chat history and add new system message
                    chatRepository.clearHistory()
                    lastUserMessage = null
                    _state.update {
                        ChatState(messages = listOf(systemMessage))
                    }
                }
            } else {
                // Normal mode - remove system messages
                if (preserveHistory) {
                    // Just remove system message, keep history
                    _state.update {
                        it.copy(messages = it.messages.filter { msg -> msg.messageType != MessageType.SYSTEM })
                    }
                } else {
                    // Clear everything
                    chatRepository.clearHistory()
                    lastUserMessage = null
                    _state.update { ChatState() }
                }
            }
        }
    }

    fun dispatch(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> sendMessage(intent.text)
            is ChatIntent.RetryLastMessage -> retryLastMessage()
            is ChatIntent.ClearChat -> clearChat()
            is ChatIntent.CopyMessage -> copyMessage(intent.messageId)
            is ChatIntent.CopyAllMessages -> copyAllMessages()
        }
    }

    private fun sendMessage(text: String) {
        if (text.isBlank()) return

        lastUserMessage = text
        coroutineScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Add user message to state immediately
                val userMessage = ChatMessage(
                    content = text,
                    isFromUser = true
                )
                _state.update { it.copy(messages = it.messages + userMessage) }

                // Get AI response
                val response = sendMessageUseCase(text)

                // Add AI response to state
                val aiMessage = ChatMessage(
                    content = response.content,
                    isFromUser = false,
                    executionTimeMs = response.executionTimeMs,
                    promptTokens = response.promptTokens,
                    completionTokens = response.completionTokens,
                    totalTokens = response.totalTokens
                )
                _state.update {
                    it.copy(
                        messages = it.messages + aiMessage,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    private fun retryLastMessage() {
        lastUserMessage?.let { message ->
            // Remove the last user message and any partial response
            val currentMessages = _state.value.messages
            val messagesToKeep = currentMessages.dropLast(1)
            _state.update { it.copy(messages = messagesToKeep, error = null) }

            // Resend the message
            sendMessage(message)
        }
    }

    private fun clearChat() {
        _state.update { ChatState() }
        chatRepository.clearHistory()
        lastUserMessage = null
    }

    private fun copyMessage(messageId: String) {
        val message = _state.value.messages.find { it.id == messageId }
        message?.let {
            ClipboardManager.copyToClipboard(it.content)
        }
    }

    private fun copyAllMessages() {
        val allMessages = _state.value.messages
            .filter { it.messageType != MessageType.SYSTEM }
            .joinToString("\n\n") { message ->
                val sender = when (message.messageType) {
                    MessageType.USER -> "User"
                    MessageType.AI -> "AI"
                    MessageType.SYSTEM -> "System"
                }
                "$sender: ${message.content}"
            }
        ClipboardManager.copyToClipboard(allMessages)
    }
}
