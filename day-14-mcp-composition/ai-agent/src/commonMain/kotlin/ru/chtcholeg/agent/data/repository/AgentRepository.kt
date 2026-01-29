package ru.chtcholeg.agent.data.repository

import io.ktor.client.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import ru.chtcholeg.agent.BuildKonfig
import ru.chtcholeg.shared.data.api.GigaChatApi
import ru.chtcholeg.shared.data.api.GigaChatApiImpl
import ru.chtcholeg.shared.data.api.HuggingFaceApi
import ru.chtcholeg.shared.data.api.HuggingFaceApiImpl
import ru.chtcholeg.shared.data.model.*
import ru.chtcholeg.agent.domain.model.*
import ru.chtcholeg.shared.domain.model.ConnectionStatus
import ru.chtcholeg.shared.domain.model.Model
import ru.chtcholeg.shared.domain.model.McpToolResult
import kotlin.time.measureTimedValue

class AgentRepository(
    httpClient: HttpClient,
    private val settingsRepository: SettingsRepository,
    private val mcpRepository: McpRepository
) {
    private val gigaChatApi: GigaChatApi = GigaChatApiImpl(httpClient)
    private val huggingFaceApi: HuggingFaceApi = HuggingFaceApiImpl(httpClient)

    private val conversationHistory = mutableListOf<Message>()
    private var gigaChatAccessToken: String? = null
    private var gigaChatTokenExpiry: Long? = null

    /**
     * Generates system prompt based on connected MCP servers.
     * Returns null if no servers are connected.
     */
    private fun buildSystemPrompt(serverCategories: List<String>): String? {
        if (serverCategories.isEmpty()) return null

        val categoriesList = serverCategories.joinToString("\n") { "- $it" }

        return """
Ты — точный и полезный AI-ассистент. Твоя задача — решать проблемы пользователя, используя доступные инструменты (API).

ПРАВИЛА РАБОТЫ:
1.  Анализируй запрос пользователя. Если для ответа нужны актуальные данные, вычисления или действия, которые ты не можешь выполнить сам, — используй инструменты.
2.  Когда получишь результат вызова инструмента, проанализируй его.
3.  Если результата достаточно для полного ответа пользователю — дай ответ.
4.  Если нужна дополнительная информация — вызови следующий инструмент (вернись к шагу 2).
5.  Если инструменты не нужны или недоступны, дай ответ, используя свои знания.
6.  Будь краток в рассуждениях. Главное — точный вызов инструмента или четкий ответ.

Доступные категории инструментов:
$categoriesList

Твой вывод ДОЛЖЕН быть либо вызовом инструмента в указанном JSON-формате, либо финальным ответом пользователю.
        """.trimIndent()
    }

    /**
     * Send a message and get AI response with metadata.
     * Supports chained function calls (multiple tools in sequence).
     */
    suspend fun sendMessage(userMessage: String): AgentMessage {
        val settings = settingsRepository.settings.value

        // Add user message to history
        conversationHistory.add(
            Message(
                role = "user",
                content = userMessage
            )
        )

        // Get connected servers and build system prompt
        val connectedServers = mcpRepository.servers.value
            .filter { it.status == ConnectionStatus.CONNECTED }
        val serverCategories = connectedServers.map { it.name }
        val systemPrompt = buildSystemPrompt(serverCategories)

        // Get available tools from MCP servers
        val availableTools = mcpRepository.getAllTools()
        val functions = availableTools.map { tool ->
            GigaChatFunction(
                name = tool.name,
                description = tool.description,
                parameters = tool.inputSchema,
                fewShotExamples = tool.fewShotExamples.map { example ->
                    FewShotExample(
                        request = example.request,
                        params = example.params
                    )
                }
            )
        }

        // Track total execution time and token usage across all calls
        var totalExecutionTimeMs = 0L
        var totalPromptTokens = 0
        var totalCompletionTokens = 0
        var totalTokens = 0

        // Loop to handle chained function calls
        val maxIterations = 10  // Safety limit to prevent infinite loops
        var iterations = 0

        while (iterations < maxIterations) {
            iterations++

            // Measure execution time for this call
            val (response, duration) = measureTimedValue {
                when (settings.model.api) {
                    Model.Api.GIGACHAT -> sendToGigaChat(settings, functions, systemPrompt)
                    Model.Api.HUGGINGFACE -> sendToHuggingFace(settings)
                }
            }

            totalExecutionTimeMs += duration.inWholeMilliseconds
            totalPromptTokens += response.usage?.promptTokens ?: 0
            totalCompletionTokens += response.usage?.completionTokens ?: 0
            totalTokens += response.usage?.totalTokens ?: 0

            val choice = response.choices.firstOrNull()
                ?: error("No response choice from AI")

            // Check if AI wants to call a function
            if (choice.finishReason == "function_call") {
                val functionCall = choice.message.functionCall
                    ?: error("Function call expected but not found")

                // Add assistant's function call message to history
                conversationHistory.add(
                    Message(
                        role = "assistant",
                        content = null,
                        functionCall = functionCall
                    )
                )

                // Execute the tool
                val toolResult = executeFunctionCall(functionCall)

                // Format function result as JSON for GigaChat
                val functionResultJson = buildJsonObject {
                    put("result", toolResult.content)
                    put("is_error", toolResult.isError)
                }.toString()

                // Add function result to history
                conversationHistory.add(
                    Message(
                        role = "function",
                        name = functionCall.name,
                        content = functionResultJson
                    )
                )

                // Continue loop to get next response (may be another function call or final answer)
                continue
            }

            // Regular response - we're done
            val aiMessage = choice.message.content ?: ""
            conversationHistory.add(
                Message(
                    role = "assistant",
                    content = aiMessage
                )
            )

            return AgentMessage(
                content = aiMessage.trim(),
                type = MessageType.AI,
                executionTimeMs = totalExecutionTimeMs,
                promptTokens = totalPromptTokens,
                completionTokens = totalCompletionTokens,
                totalTokens = totalTokens
            )
        }

        // Safety: if we hit max iterations, return error
        return AgentMessage(
            content = "Error: Too many function calls in chain (max $maxIterations)",
            type = MessageType.AI,
            executionTimeMs = totalExecutionTimeMs,
            promptTokens = totalPromptTokens,
            completionTokens = totalCompletionTokens,
            totalTokens = totalTokens
        )
    }

    private suspend fun executeFunctionCall(functionCall: FunctionCall): McpToolResult {
        return try {
            val result = mcpRepository.executeTool(
                toolName = functionCall.name,
                arguments = functionCall.arguments
            )
            result.getOrThrow()
        } catch (e: Exception) {
            McpToolResult(
                content = "Error executing tool: ${e.message}",
                isError = true
            )
        }
    }

    private suspend fun sendToGigaChat(
        settings: AiSettings,
        functions: List<GigaChatFunction>,
        systemPrompt: String?
    ): ChatResponse {
        // Check token expiry and re-authenticate if needed
        val now = Clock.System.now().toEpochMilliseconds()
        if (gigaChatAccessToken == null || gigaChatTokenExpiry == null || now >= gigaChatTokenExpiry!!) {
            val authResponse = gigaChatApi.authenticate(
                clientId = BuildKonfig.GIGACHAT_CLIENT_ID,
                clientSecret = BuildKonfig.GIGACHAT_CLIENT_SECRET
            )
            gigaChatAccessToken = authResponse.accessToken
            gigaChatTokenExpiry = authResponse.expiresAt
        }

        // Build messages with optional system prompt
        val messages = if (systemPrompt != null) {
            listOf(Message(role = "system", content = systemPrompt)) + conversationHistory.toList()
        } else {
            conversationHistory.toList()
        }

        return gigaChatApi.sendMessage(
            accessToken = gigaChatAccessToken!!,
            messages = messages,
            model = settings.model.id,
            temperature = settings.temperature,
            topP = settings.topP,
            maxTokens = settings.maxTokens,
            repetitionPenalty = settings.repetitionPenalty,
            functions = functions.takeIf { it.isNotEmpty() }
        )
    }

    private suspend fun sendToHuggingFace(settings: AiSettings): ChatResponse {
        return huggingFaceApi.sendMessage(
            accessToken = BuildKonfig.HUGGINGFACE_API_TOKEN,
            messages = conversationHistory.toList(),
            model = settings.model.id,
            temperature = settings.temperature,
            topP = settings.topP,
            maxTokens = settings.maxTokens,
            repetitionPenalty = settings.repetitionPenalty
        )
    }

    fun clearHistory() {
        conversationHistory.clear()
    }
}
