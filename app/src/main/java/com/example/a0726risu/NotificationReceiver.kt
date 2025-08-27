package com.example.a0726risu

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

data class NotificationInfo(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val message: String,
    val timestamp: String
)

object NotificationRepository {
    private val _notifications = MutableStateFlow<List<NotificationInfo>>(emptyList())
    val notifications: StateFlow<List<NotificationInfo>> = _notifications

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
        loadNotifications()
    }

    fun addNotification(title: String, message: String) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val newNotification = NotificationInfo(title = title, message = message, timestamp = timestamp)
        _notifications.value = listOf(newNotification) + _notifications.value
        saveNotifications()
    }

    private fun saveNotifications() {
        if (!::sharedPreferences.isInitialized) return
        repositoryScope.launch {
            val jsonString = gson.toJson(_notifications.value)
            sharedPreferences.edit {
                putString("notification_list_key", jsonString)
            }
        }
    }

    private fun loadNotifications() {
        if (!::sharedPreferences.isInitialized) return
        repositoryScope.launch {
            val jsonString = sharedPreferences.getString("notification_list_key", null)
            if (jsonString != null) {
                val type = object : TypeToken<List<NotificationInfo>>() {}.type
                _notifications.value = gson.fromJson(jsonString, type)
            }
        }
    }
}

const val CHANNEL_ID = "call_notification_channel"
const val EXTRA_TITLE = "extra_title"
const val EXTRA_MESSAGE = "extra_message"
const val EXTRA_CHANNEL_NAME = "extra_channel_name"
const val EXTRA_TOKEN = "extra_token"

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "時間です！"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "通話の準備をしましょう。"
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: return
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: ""

        NotificationRepository.addNotification(title, message)

        val fullScreenIntent = Intent(context, VideoCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("CHANNEL_NAME", channelName)
            putExtra("TOKEN", token)
        }

        val requestCode = System.currentTimeMillis().toInt()
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_call)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(requestCode, notificationBuilder.build())
    }
}