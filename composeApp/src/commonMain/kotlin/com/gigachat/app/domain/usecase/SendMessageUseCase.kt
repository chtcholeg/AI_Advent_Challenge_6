package com.gigachat.app.domain.usecase

import com.gigachat.app.data.repository.ChatRepository

class SendMessageUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(message: String): String {
        require(message.isNotBlank()) { "Message cannot be empty" }
        return repository.sendMessage(message.trim())
    }
}
