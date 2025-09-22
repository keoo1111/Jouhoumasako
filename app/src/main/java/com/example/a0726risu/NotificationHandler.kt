package com.example.a0726risu

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.a0726risu.AppConstants.CHANNEL_ID
import com.example.a0726risu.AppConstants.EXTRA_CHANNEL_NAME
import com.example.a0726risu.AppConstants.EXTRA_DAY_OF_WEEK
import com.example.a0726risu.AppConstants.EXTRA_MESSAGE
import com.example.a0726risu.AppConstants.EXTRA_TIME
import com.example.a0726risu.AppConstants.EXTRA_TITLE
import com.example.a0726risu.AppConstants.EXTRA_TOKEN

// --- 通知受信レシーバー ---

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "時間です！"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "通話の準備をしましょう。"
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: return
        val time = intent.getStringExtra(EXTRA_TIME) ?: ""
        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, 0)
        val notificationId = (channelName + dayOfWeek + time).hashCode()

        NotificationRepository.addNotification(title, message)

        val fullScreenIntent = Intent(context, VideoCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("CHANNEL_NAME", channelName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CHANNEL_NAME, channelName)
            putExtra("TIME", time)
            putExtra("DAY_OF_WEEK", dayOfWeek)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            snoozeIntent,
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
            .addAction(0, "10 分後に再通知", snoozePendingIntent)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notificationBuilder.build())
    }
}

// --- スヌーズ（再通知）処理レシーバー ---

private const val SNOOZE_DURATION_MINUTES = 10L

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "通話リマインダー"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "時間です"
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: ""
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: ""
        val time = intent.getStringExtra("TIME") ?: ""
        val dayOfWeek = intent.getIntExtra("DAY_OF_WEEK", 0)
        val notificationId = (channelName + dayOfWeek + time).hashCode()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        val snoozeTimeMillis = System.currentTimeMillis() + SNOOZE_DURATION_MINUTES * 60 * 1000
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_CHANNEL_NAME, channelName)
            putExtra(EXTRA_TOKEN, token)
            putExtra("TIME", time)
            putExtra("DAY_OF_WEEK", dayOfWeek)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "再通知のための権限がありません。", Toast.LENGTH_SHORT).show()
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, snoozePendingIntent)
        Toast.makeText(context, "10 分後に再通知します", Toast.LENGTH_SHORT).show()
    }
}