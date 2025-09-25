package com.example.a0726risu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@Composable
fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier) {
        Text(text = text)
    }
}

@Composable
fun NavigationBar(onHomeClick: () -> Unit, onNotificationsClick: () -> Unit, onSettingsClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.NavigationBar(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF6200EE)).padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(text = "ホーム", onClick = onHomeClick)
            ActionButton(text = "通知", onClick = onNotificationsClick)
            ActionButton(text = "設定", onClick = onSettingsClick)
        }
    }
}

@Composable
fun MyTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    TextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier)
}

@Composable
fun CallInfoItem(info: CallInfo, onVideoCall: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    fun dayOfWeekToString(day: Int): String {
        return when (day) {
            Calendar.SUNDAY -> "日曜日"; Calendar.MONDAY -> "月曜日"; Calendar.TUESDAY -> "火曜日"; Calendar.WEDNESDAY -> "水曜日";
            Calendar.THURSDAY -> "木曜日"; Calendar.FRIDAY -> "金曜日"; Calendar.SATURDAY -> "土曜日"; else -> "不明"
        }
    }
    Card(modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("名前：${info.name}", style = MaterialTheme.typography.titleMedium)
            Text("ルーム ID：${info.number}", style = MaterialTheme.typography.bodyMedium)
            val daysText = info.daysOfWeek.sorted().map { dayOfWeekToString(it) }.joinToString(", ")
            Text("時間：毎週$daysText ${info.time}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onEdit) { Text("編集") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onVideoCall) { Text("通話を開始する") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("削除")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOfWeekSelector(selectedDays: Set<String>, onDaySelected: (String) -> Unit, daysOfWeek: List<String>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = if (selectedDays.isEmpty()) "曜日を選択" else selectedDays.sorted().joinToString(", ")
    Box(modifier = modifier) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                value = displayText,
                onValueChange = {},
                readOnly = true,
                label = { Text("曜日") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                daysOfWeek.forEach { day ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onDaySelected(day) }.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = selectedDays.contains(day), onCheckedChange = { onDaySelected(day) })
                        Spacer(Modifier.width(8.dp))
                        Text(day)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(info: NotificationInfo, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = info.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = info.message, style = MaterialTheme.typography.bodyMedium)
            Text(text = info.timestamp, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun VideoCallUi(
    statusText: String,
    hasRemoteUser: Boolean,
    onCallEnd: () -> Unit,
    localSurfaceView: @Composable () -> Unit,
    remoteSurfaceView: @Composable () -> Unit,
    topics: List<String>
) {
    var currentTopic by remember {
        mutableStateOf(topics.firstOrNull() ?: "話題が見つかりませんでした。")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasRemoteUser) {
            remoteSurfaceView()
        } else {
            localSurfaceView()
        }

        if (hasRemoteUser) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(120.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
            ) {
                localSurfaceView()
            }
        }
        if (!hasRemoteUser) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // 各要素間のスペース
        ) {
            Text(
                text = currentTopic,
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )

            Button(
                onClick = {
                    val otherTopics = topics.filter { it != currentTopic }
                    currentTopic = if (otherTopics.isNotEmpty()) {
                        otherTopics.random()
                    } else {
                        topics.randomOrNull() ?: "話題がありません"
                    }
                }
            ) {
                Text("次の話題")
            }

            Button(
                onClick = onCallEnd,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("通話を終了する")
            }
        }
    }
}