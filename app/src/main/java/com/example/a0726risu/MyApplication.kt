package com.example.a0726risu

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // アプリ起動時にリポジトリを初期化する
        NotificationRepository.init(this)
    }
}