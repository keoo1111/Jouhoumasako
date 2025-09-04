package com.example.a0726risu

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.a0726risu.AppConstants.CHANNEL_ID
import com.example.a0726risu.AppConstants.EXTRA_CHANNEL_NAME
import com.example.a0726risu.AppConstants.EXTRA_DAY_OF_WEEK
import com.example.a0726risu.AppConstants.EXTRA_MESSAGE
import com.example.a0726risu.AppConstants.EXTRA_TIME
import com.example.a0726risu.AppConstants.EXTRA_TITLE
import com.example.a0726risu.AppConstants.EXTRA_TOKEN

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Intent からすべての情報を受け取る
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "時間です！"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "通話の準備をしましょう。"
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: return
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: ""
        val time = intent.getStringExtra(EXTRA_TIME) ?: ""
        val dayOfWeek = intent.getIntExtra(EXTRA_DAY_OF_WEEK, 0)

        // 1. 安定した通知 ID を生成する
        val notificationId = (channelName + dayOfWeek + time).hashCode()

        // 2. 通知履歴を保存する
        NotificationRepository.addNotification(title, message)

        // 3. 通知タップ時のインテント (VideoCallActivity を開く)
        val fullScreenIntent = Intent(context, VideoCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("CHANNEL_NAME", channelName)
            putExtra("TOKEN", token)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // 安定した ID を使う
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. スヌーズボタン用のインテント
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CHANNEL_NAME, channelName)
            putExtra(EXTRA_TOKEN, token)
            // SnoozeReceiver が受け取るキーに合わせて調整
            putExtra("TIME", time)
            putExtra("DAY_OF_WEEK", dayOfWeek)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1, // ID が衝突しないように +1 する
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 5. 通知を組み立てる
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_call) // TODO: res/drawableにアイコンを追加してください
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

        // 6. 通知を表示する
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notificationBuilder.build()) // 安定した ID を使う
    }
}