package com.gigachat.app.data.repository

import com.gigachat.app.domain.model.AiSettings
import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    /**
     * Get current AI settings
     */
    val settings: StateFlow<AiSettings>

    /**
     * Update AI settings
     */
    fun updateSettings(settings: AiSettings)

    /**
     * Reset to default settings
     */
    fun resetToDefaults()
}
