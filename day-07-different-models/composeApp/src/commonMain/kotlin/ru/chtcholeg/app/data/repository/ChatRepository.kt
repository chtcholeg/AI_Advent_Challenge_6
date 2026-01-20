package ru.chtcholeg.app.data.repository

import ru.chtcholeg.app.domain.model.AiResponse

interface ChatRepository {
    /**
     * Send a message to the AI and get a response
     * @param userMessage The message from the user
     * @return The AI's response with metadata (execution time, tokens)
     */
    suspend fun sendMessage(userMessage: String): AiResponse

    /**
     * Clear conversation history
     */
    fun clearHistory()
}
