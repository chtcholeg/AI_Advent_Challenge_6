package com.gigachat.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.gigachat.app.presentation.chat.ChatScreen
import com.gigachat.app.presentation.chat.ChatStore
import com.gigachat.app.presentation.settings.SettingsScreen
import org.koin.compose.koinInject

enum class Screen {
    CHAT,
    SETTINGS
}

@Composable
fun App() {
    MaterialTheme {
        var currentScreen by remember { mutableStateOf(Screen.CHAT) }
        val store: ChatStore = koinInject()

        when (currentScreen) {
            Screen.CHAT -> ChatScreen(
                store = store,
                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
            )
            Screen.SETTINGS -> SettingsScreen(
                onNavigateBack = { currentScreen = Screen.CHAT }
            )
        }
    }
}
