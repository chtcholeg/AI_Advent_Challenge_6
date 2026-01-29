package ru.chtcholeg.agent.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.chtcholeg.agent.domain.model.AgentMessage
import ru.chtcholeg.agent.domain.model.MessageType

/**
 * Console-style message display.
 * Shows messages as plain text like in a terminal.
 */
@Composable
fun MessageItem(
    message: AgentMessage,
    modifier: Modifier = Modifier
) {
    val prefix = when (message.type) {
        MessageType.USER -> "> "
        MessageType.AI -> ""
        MessageType.TOOL_CALL -> "[tool] "
        MessageType.TOOL_RESULT -> "[result] "
        MessageType.SYSTEM -> "[system] "
        MessageType.ERROR -> "[error] "
    }

    val prefixColor = when (message.type) {
        MessageType.USER -> Color(0xFF6CB6FF)  // Blue
        MessageType.AI -> Color(0xFFE6E6E6)     // Light gray
        MessageType.TOOL_CALL -> Color(0xFFFFD866)  // Yellow
        MessageType.TOOL_RESULT -> Color(0xFFA9DC76)  // Green
        MessageType.SYSTEM -> Color(0xFFAB9DF2)  // Purple
        MessageType.ERROR -> Color(0xFFFF6188)  // Red
    }

    val textColor = when (message.type) {
        MessageType.USER -> Color(0xFFE6E6E6)
        MessageType.AI -> Color(0xFFE6E6E6)
        MessageType.TOOL_CALL -> Color(0xFFB0B0B0)
        MessageType.TOOL_RESULT -> Color(0xFFB0B0B0)
        MessageType.SYSTEM -> Color(0xFF909090)
        MessageType.ERROR -> Color(0xFFFF6188)
    }

    // Add extra bottom padding after AI responses to separate Q&A pairs
    val bottomPadding = when (message.type) {
        MessageType.AI -> 14.dp
        MessageType.TOOL_RESULT -> 10.dp
        else -> 2.dp
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = bottomPadding)
    ) {
        Text(
            text = buildAnnotatedString {
                if (prefix.isNotEmpty()) {
                    withStyle(SpanStyle(color = prefixColor, fontWeight = FontWeight.Bold)) {
                        append(prefix)
                    }
                }
                withStyle(SpanStyle(color = textColor)) {
                    append(message.content)
                }
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        // Metadata for AI messages (tokens, time) - subtle gray
        if (message.type == MessageType.AI && (message.executionTimeMs != null || message.totalTokens != null)) {
            val metaText = buildString {
                message.executionTimeMs?.let { ms ->
                    append(formatExecutionTime(ms))
                }
                if (message.totalTokens != null && message.promptTokens != null && message.completionTokens != null) {
                    if (isNotEmpty()) append(" · ")
                    append("${message.totalTokens} tokens (↑${message.promptTokens} ↓${message.completionTokens})")
                }
            }
            if (metaText.isNotEmpty()) {
                Text(
                    text = metaText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF606060),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun formatExecutionTime(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "%.1fs".format(ms / 1000.0)
        else -> {
            val minutes = ms / 60000
            val seconds = (ms % 60000) / 1000
            "${minutes}m ${seconds}s"
        }
    }
}
