package com.gigachat.app.data.api

import com.gigachat.app.data.model.ChatRequest
import com.gigachat.app.data.model.ChatResponse

interface HuggingFaceApi {
    suspend fun sendMessage(
        accessToken: String,
        messages: List<com.gigachat.app.data.model.Message>,
        model: String,
        temperature: Float?,
        topP: Float?,
        maxTokens: Int?,
        repetitionPenalty: Float?
    ): ChatResponse
}
