package com.example.a0726risu

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NotificationInfo(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val message: String,
    val timestamp: String
)

object NotificationRepository {
    private val _notifications = MutableStateFlow<List<NotificationInfo>>(emptyList())
    val notifications: StateFlow<List<NotificationInfo>> = _notifications

    fun addNotification(title: String, message: String) {
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val timestamp = sdf.format(Date())
        val newNotification = NotificationInfo(title = title, message = message, timestamp = timestamp)
        _notifications.value = listOf(newNotification) + _notifications.value
    }
}

const val CHANNEL_ID = "call_notification_channel"
const val EXTRA_TITLE = "extra_title"
const val EXTRA_MESSAGE = "extra_message"

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "時間です！"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "通話の準備をしましょう。"
        NotificationRepository.addNotification(title, message)
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            pendingIntentFlag
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}