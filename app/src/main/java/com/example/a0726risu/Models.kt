package com.example.a0726risu

import java.util.UUID

// このファイルにデータクラスをすべて集約します

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