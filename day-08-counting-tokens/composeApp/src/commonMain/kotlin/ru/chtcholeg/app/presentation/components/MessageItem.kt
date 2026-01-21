package ru.chtcholeg.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.chtcholeg.app.domain.model.ChatMessage
import ru.chtcholeg.app.domain.model.MessageType
import ru.chtcholeg.app.domain.model.StructuredResponse
import ru.chtcholeg.app.domain.model.StructuredXmlResponse
import ru.chtcholeg.app.presentation.theme.ChatColors

@Composable
fun MessageItem(
    message: ChatMessage,
    onCopyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // System messages are displayed centered
    val arrangement = when (message.messageType) {
        MessageType.SYSTEM -> Arrangement.Center
        MessageType.USER -> Arrangement.End
        MessageType.AI -> Arrangement.Start
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = arrangement
    ) {
        BoxWithConstraints {
            Box(
                modifier = Modifier
                    .widthIn(max = if (message.messageType == MessageType.SYSTEM) maxWidth * 0.9f else maxWidth * 0.75f)
                    .background(
                        color = when (message.messageType) {
                            MessageType.USER -> ChatColors.UserBubbleBackground
                            MessageType.AI -> ChatColors.AiBubbleBackground
                            MessageType.SYSTEM -> ChatColors.SystemBubbleBackground
                        },
                        shape = when (message.messageType) {
                            MessageType.SYSTEM -> RoundedCornerShape(16.dp)
                            MessageType.USER -> RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 4.dp
                            )
                            MessageType.AI -> RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 16.dp
                            )
                        }
                    )
                    .padding(12.dp)
            ) {
                val alignment = when (message.messageType) {
                    MessageType.USER -> Alignment.CenterEnd
                    MessageType.AI -> Alignment.CenterStart
                    MessageType.SYSTEM -> Alignment.Center
                }

                Column(modifier = Modifier.align(alignment)) {
                    // Check if this is a structured JSON or XML response
                    val isJsonResponse = message.messageType == MessageType.AI &&
                        StructuredResponse.looksLikeStructuredResponse(message.content)
                    val isXmlResponse = message.messageType == MessageType.AI &&
                        StructuredXmlResponse.looksLikeStructuredXmlResponse(message.content)

                    when {
                        isJsonResponse -> StructuredJsonMessageContent(message = message)
                        isXmlResponse -> StructuredXmlMessageContent(message = message)
                        else -> {
                            // Regular message display
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = when (message.messageType) {
                                    MessageType.USER -> ChatColors.UserBubbleText
                                    MessageType.AI -> ChatColors.AiBubbleText
                                    MessageType.SYSTEM -> ChatColors.SystemBubbleText
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Show execution time and tokens for AI messages
                    if (message.messageType == MessageType.AI && (message.executionTimeMs != null || message.totalTokens != null)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val metadataColor = ChatColors.AiBubbleTimestamp

                            message.executionTimeMs?.let { timeMs ->
                                Text(
                                    text = formatExecutionTime(timeMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = metadataColor
                                )
                            }

                            if (message.executionTimeMs != null && message.totalTokens != null) {
                                Text(
                                    text = " | ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = metadataColor
                                )
                            }

                            message.totalTokens?.let { total ->
                                val tokensText = buildString {
                                    append("$total tokens")
                                    if (message.promptTokens != null && message.completionTokens != null) {
                                        append(" (${message.promptTokens} prompt + ${message.completionTokens} completion)")
                                    }
                                }
                                Text(
                                    text = tokensText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = metadataColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (message.messageType) {
                                MessageType.USER -> ChatColors.UserBubbleTimestamp
                                MessageType.AI -> ChatColors.AiBubbleTimestamp
                                MessageType.SYSTEM -> ChatColors.SystemBubbleTimestamp
                            },
                        )

                        IconButton(
                            onClick = { onCopyMessage(message.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Copy message",
                                modifier = Modifier.size(16.dp),
                                tint = when (message.messageType) {
                                    MessageType.USER -> ChatColors.UserBubbleTimestamp
                                    MessageType.AI -> ChatColors.AiBubbleTimestamp
                                    MessageType.SYSTEM -> ChatColors.SystemBubbleTimestamp
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StructuredJsonMessageContent(message: ChatMessage) {
    var showFormatted by remember { mutableStateOf(false) }
    val structuredResponse = remember(message.content, showFormatted) {
        if (showFormatted) StructuredResponse.tryParse(message.content) else null
    }

    Column {
        // Toggle button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showFormatted) "Formatted View" else "JSON View",
                style = MaterialTheme.typography.labelMedium,
                color = ChatColors.AiBubbleText.copy(alpha = 0.7f)
            )
            TextButton(
                onClick = { showFormatted = !showFormatted }
            ) {
                Text(
                    text = if (showFormatted) "Show JSON" else "Format",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChatColors.AiBubbleText
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content display
        if (showFormatted && structuredResponse != null) {
            // Formatted view
            FormattedStructuredContent(
                unicodeSymbols = structuredResponse.unicodeSymbols,
                questionShort = structuredResponse.questionShort,
                answer = structuredResponse.response,
                responderRole = structuredResponse.responderRole
            )
        } else {
            // JSON view (or failed to parse)
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = ChatColors.AiBubbleText
            )

            if (showFormatted && structuredResponse == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Failed to parse JSON",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChatColors.AiBubbleText.copy(alpha = 0.5f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun StructuredXmlMessageContent(message: ChatMessage) {
    var showFormatted by remember { mutableStateOf(false) }
    val structuredResponse = remember(message.content, showFormatted) {
        if (showFormatted) StructuredXmlResponse.tryParse(message.content) else null
    }

    Column {
        // Toggle button row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showFormatted) "Formatted View" else "XML View",
                style = MaterialTheme.typography.labelMedium,
                color = ChatColors.AiBubbleText.copy(alpha = 0.7f)
            )
            TextButton(
                onClick = { showFormatted = !showFormatted }
            ) {
                Text(
                    text = if (showFormatted) "Show XML" else "Format",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChatColors.AiBubbleText
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Content display
        if (showFormatted && structuredResponse != null) {
            // Formatted view
            FormattedStructuredContent(
                unicodeSymbols = structuredResponse.unicodeSymbols,
                questionShort = structuredResponse.questionShort,
                answer = structuredResponse.answer,
                responderRole = structuredResponse.responderRole
            )
        } else {
            // XML view (or failed to parse)
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = ChatColors.AiBubbleText
            )

            if (showFormatted && structuredResponse == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Failed to parse XML",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChatColors.AiBubbleText.copy(alpha = 0.5f),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun FormattedStructuredContent(
    unicodeSymbols: String,
    questionShort: String,
    answer: String,
    responderRole: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Unicode symbols
        Text(
            text = unicodeSymbols,
            style = MaterialTheme.typography.headlineMedium,
            color = ChatColors.AiBubbleText
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Question short
        Row {
            Text(
                text = "Вопрос коротко: ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ChatColors.AiBubbleText
            )
            Text(
                text = questionShort,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = ChatColors.AiBubbleText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Answer
        Column {
            Text(
                text = "Ответ:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ChatColors.AiBubbleText
            )
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = ChatColors.AiBubbleText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Responder role
        Row {
            Text(
                text = "Ответил на него: ",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ChatColors.AiBubbleText
            )
            Text(
                text = responderRole,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = ChatColors.AiBubbleText
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:${localDateTime.minute.toString().padStart(2, '0')}"
}

private fun formatExecutionTime(milliseconds: Long): String {
    return when {
        milliseconds < 1000 -> "${milliseconds}ms"
        milliseconds < 60000 -> {
            val seconds = milliseconds / 1000
            val decimal = (milliseconds % 1000) / 100
            "${seconds}.${decimal}s"
        }
        else -> {
            val minutes = milliseconds / 60000
            val seconds = (milliseconds % 60000) / 1000
            "${minutes}m ${seconds}s"
        }
    }
}
