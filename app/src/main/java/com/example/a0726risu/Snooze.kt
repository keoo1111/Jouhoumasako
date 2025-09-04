package com.example.a0726risu

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.a0726risu.AppConstants.EXTRA_CHANNEL_NAME
import com.example.a0726risu.AppConstants.EXTRA_DAY_OF_WEEK
import com.example.a0726risu.AppConstants.EXTRA_MESSAGE
import com.example.a0726risu.AppConstants.EXTRA_TIME
import com.example.a0726risu.AppConstants.EXTRA_TITLE
import com.example.a0726risu.AppConstants.EXTRA_TOKEN

private const val SNOOZE_DURATION_MINUTES = 10L

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 元の通知から渡された情報を取得
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "通話リマインダー"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "時間です"
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: ""
        val token = intent.getStringExtra(EXTRA_TOKEN) ?: ""
        val time = intent.getStringExtra("TIME") ?: ""
        val dayOfWeek = intent.getIntExtra("DAY_OF_WEEK", 0)

        // どの通知を消すかを特定するための ID
        val notificationId = (channelName + dayOfWeek + time).hashCode()

        // 元の通知を画面から消去する
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        // 10 分後に新しいアラームをスケジュールする
        val snoozeTimeMillis = System.currentTimeMillis() + SNOOZE_DURATION_MINUTES * 60 * 1000

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // 再度、NotificationReceiver を呼び出すための Intent を作成
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
            notificationId + 1, // 元の ID と衝突しないようにリクエストコードをずらす
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "再通知のための権限がありません。", Toast.LENGTH_SHORT).show()
            return
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            snoozeTimeMillis,
            snoozePendingIntent
        )

        // ユーザーへのメッセージを修正
        Toast.makeText(context, "10 分後に再通知します", Toast.LENGTH_SHORT).show()
    }
}