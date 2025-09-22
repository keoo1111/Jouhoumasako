package com.example.a0726risu

import android.app.Application

// --- アプリケーションクラス ---

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationRepository.init(this)
    }
}

// --- 定数オブジェクト ---

object AppConstants {
    // Agora 関連
    const val AGORA_TOKEN = "007eJxTYHiyrf7nHb84k6a7sScLDsm9kIo+vmAuL9/5klP2QTJrUhcqMKSkmCSmGhumpqRYmppYmBtamlsaGZslp6WZppokGyYl1+88nNEQyMjw5+NjZkYGCATxmRkMDQ0ZGADKgCFv"
    const val AGORA_APP_ID = "dd4ae31edd954871979236cff5e4c1bcdd4ae31edd954871979236cff5e4c1bc"

    // 通知チャンネル ID
    const val CHANNEL_ID = "call_notification_channel"

    // Intent で使うキー (EXTRA)
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"
    const val EXTRA_CHANNEL_NAME = "extra_channel_name"
    const val EXTRA_TOKEN = "extra_token"
    const val EXTRA_TIME = "extra_time" // 時間
    const val EXTRA_DAY_OF_WEEK = "extra_day_of_week" // 曜日
    const val PREFS_SETTINGS = "settings_prefs"
    const val KEY_FONT_SIZE = "font_size_key"

    const val TAG_VC = "VideoCallActivity"
}