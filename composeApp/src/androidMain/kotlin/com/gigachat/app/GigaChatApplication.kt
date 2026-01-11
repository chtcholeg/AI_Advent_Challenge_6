package com.gigachat.app

import android.app.Application
import com.gigachat.app.di.initKoin

class GigaChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Koin once for the entire app lifecycle
        initKoin()
    }
}
