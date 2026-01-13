package ru.chtcholeg.app.domain.model

import kotlinx.serialization.Serializable

/**
 * AI model configuration settings
 */
@Serializable
data class AiSettings(
    val model: String = DEFAULT_MODEL,
    val temperature: Float? = DEFAULT_TEMPERATURE,
    val topP: Float? = DEFAULT_TOP_P,
    val maxTokens: Int? = DEFAULT_MAX_TOKENS,
    val repetitionPenalty: Float? = DEFAULT_REPETITION_PENALTY,
    val useStructuredResponse: Boolean = false,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_MODEL = "GigaChat"
        const val DEFAULT_TEMPERATURE = 0.7f
        const val DEFAULT_TOP_P = 0.9f
        const val DEFAULT_MAX_TOKENS = 2048
        const val DEFAULT_REPETITION_PENALTY = 1.0f

        // Valid ranges
        const val MIN_TEMPERATURE = 0.0f
        const val MAX_TEMPERATURE = 2.0f
        const val MIN_TOP_P = 0.0f
        const val MAX_TOP_P = 1.0f
        const val MIN_MAX_TOKENS = 1
        const val MAX_MAX_TOKENS = 8192
        const val MIN_REPETITION_PENALTY = 0.0f
        const val MAX_REPETITION_PENALTY = 2.0f

        const val DEFAULT_SYSTEM_PROMPT = """You are an AI assistant that MUST ALWAYS respond in a strict JSON format. This is a critical requirement - your entire response must be valid JSON that can be parsed without any errors.

CRITICAL RULES YOU MUST FOLLOW:
1. Your ENTIRE response must be ONLY valid JSON - no text before or after the JSON
2. DO NOT include markdown code blocks (no ```json or ``` markers)
3. DO NOT include any explanatory text outside the JSON structure
4. All string values must use proper JSON escaping for special characters (quotes, newlines, etc.)
5. Ensure all braces, brackets, and quotes are properly closed

You must respond in this exact JSON structure:
{
  "question_short": "A brief one-line summary of the user's question (max 60 characters)",
  "response": "Your detailed answer to the user's question (properly escaped JSON string)",
  "responder_role": "The type of expert who would best answer this question (e.g., 'Software Engineer', 'Doctor', 'Historian')",
  "unicode_symbols": "3-5 relevant unicode emoji/symbols related to the question (e.g., '🔧💻📱')"
}

EXAMPLE OF CORRECT RESPONSE:
{
  "question_short": "How to make pizza?",
  "response": "To make pizza: 1) Prepare dough with flour, water, yeast, salt. 2) Let it rise for 1-2 hours. 3) Roll out the dough. 4) Add tomato sauce, cheese, and toppings. 5) Bake at 250°C (480°F) for 10-15 minutes until golden.",
  "responder_role": "Chef",
  "unicode_symbols": "🍕👨‍🍳🔥"
}

Remember: Your response must be ONLY the JSON object, nothing else. No explanations, no markdown, just pure valid JSON."""

        val DEFAULT = AiSettings()
    }

    /**
     * Validate and clamp settings to valid ranges
     */
    fun validated(): AiSettings = copy(
        temperature = temperature?.coerceIn(MIN_TEMPERATURE, MAX_TEMPERATURE),
        topP = topP?.coerceIn(MIN_TOP_P, MAX_TOP_P),
        maxTokens = maxTokens?.coerceIn(MIN_MAX_TOKENS, MAX_MAX_TOKENS),
        repetitionPenalty = repetitionPenalty?.coerceIn(MIN_REPETITION_PENALTY, MAX_REPETITION_PENALTY)
    )
}
