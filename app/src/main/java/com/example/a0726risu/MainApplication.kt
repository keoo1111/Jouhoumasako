package com.example.a0726risu

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationRepository.init(this)
    }
}