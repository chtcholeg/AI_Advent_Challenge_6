package ru.chtcholeg.app.util

import kotlinx.browser.window

actual object ClipboardManager {
    actual fun copyToClipboard(text: String) {
        window.navigator.clipboard.writeText(text)
    }
}
