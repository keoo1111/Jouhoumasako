package com.example.a0726risu

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class FontSize(val scale: Float, val displayName: String) {
    SMALL(0.8f, "小"),
    MEDIUM(1.0f, "中"),
    LARGE(1.2f, "大")
}


// --- データクラス ---

data class CallInfo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String,
    val time: String,
    val daysOfWeek: Set<Int>
)

data class NotificationInfo(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val message: String,
    val timestamp: String
)

// --- 通知リポジトリ ---

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