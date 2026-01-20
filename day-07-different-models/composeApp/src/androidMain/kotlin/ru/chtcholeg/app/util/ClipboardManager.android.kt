package ru.chtcholeg.app.util

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual object ClipboardManager : KoinComponent {
    private val context: Context by inject()

    actual fun copyToClipboard(text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
        val clip = ClipData.newPlainText("message", text)
        clipboardManager.setPrimaryClip(clip)
    }
}
