package ru.chtcholeg.app.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.chtcholeg.app.domain.model.MessageType
import ru.chtcholeg.app.presentation.components.MessageInput
import ru.chtcholeg.app.presentation.components.MessageList
import ru.chtcholeg.app.presentation.theme.ChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    store: ChatStore,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by store.state.collectAsState()
    var showSummarizeDialog by remember { mutableStateOf(false) }

    // Summarization dialog
    if (showSummarizeDialog) {
        AlertDialog(
            onDismissRequest = { showSummarizeDialog = false },
            title = { Text("Summarize Conversation") },
            text = {
                Column {
                    Text("Choose how to summarize the conversation:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add Summary - keeps the current chat and adds a summary at the end",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Replace with Summary - replaces the entire chat with the summary, allowing you to continue the conversation based on it",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSummarizeDialog = false
                        store.dispatch(ChatIntent.SummarizeAndReplaceChat)
                    }
                ) {
                    Text("Replace with Summary")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSummarizeDialog = false
                        store.dispatch(ChatIntent.SummarizeChat)
                    }
                ) {
                    Text("Add Summary")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("AI Chat")
                        if (state.currentModelName.isNotEmpty()) {
                            Text(
                                text = state.currentModelName,
                                style = MaterialTheme.typography.bodySmall,
                                color = ChatColors.HeaderText.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(
                        onClick = { showSummarizeDialog = true },
                        enabled = state.messages.filter { it.messageType != MessageType.SYSTEM }.size >= 2 && !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "Summarize conversation"
                        )
                    }
                    IconButton(
                        onClick = { store.dispatch(ChatIntent.CopyAllMessages) },
                        enabled = state.messages.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Copy all messages"
                        )
                    }
                    IconButton(
                        onClick = { store.dispatch(ChatIntent.ClearChat) },
                        enabled = state.messages.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear chat"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ChatColors.HeaderBackground,
                    titleContentColor = ChatColors.HeaderText,
                    actionIconContentColor = ChatColors.HeaderText
                )
            )
        },
        bottomBar = {
            Column {
                // Show loading indicator
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Show error message with retry button
                state.error?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { store.dispatch(ChatIntent.RetryLastMessage) }
                            ) {
                                Text(
                                    "Retry",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                MessageInput(
                    onSendMessage = { message ->
                        store.dispatch(ChatIntent.SendMessage(message))
                    },
                    isLoading = state.isLoading
                )
            }
        }
    ) { paddingValues ->
        MessageList(
            messages = state.messages,
            onCopyMessage = { messageId ->
                store.dispatch(ChatIntent.CopyMessage(messageId))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}
