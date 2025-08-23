package com.example.a0726risu.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a0726risu.ActionButton
import com.example.a0726risu.CallInfo
import com.example.a0726risu.CallInfoItem
import com.example.a0726risu.DayOfWeekSelector
import com.example.a0726risu.MyTextField
import com.example.a0726risu.NavigationBar
import com.example.a0726risu.NotificationInfo
import com.example.a0726risu.NotificationItem
import com.example.a0726risu.VideoCallUi
import java.util.Calendar

@Preview(showBackground = true)
@Composable
fun Screen1Preview() {
    _0726risuTheme {
        val sampleList = listOf(
            CallInfo(name = "たまがけ", number = "090-xxxx-xxxx", time = "18:00", daysOfWeek = setOf(Calendar.SATURDAY)),
            CallInfo(name = "こうりん", number = "080-7706-2723", time = "19:00", daysOfWeek = setOf(Calendar.MONDAY, Calendar.FRIDAY))
        )
        Scaffold(
            bottomBar = {
                NavigationBar(onHomeClick = {}, onNotificationsClick = {}, onSettingsClick = {})
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "リスト",
                        fontSize = 24.sp,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sampleList) { info ->
                            CallInfoItem(
                                info = info,
                                onVideoCall = {},
                                onEdit = {},
                                onDelete = {}
                            )
                        }
                    }
                }
                ActionButton(
                    text = "＋",
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 16.dp)
                )
            }
        }
    }
}



@Preview(showBackground = true, name = "ビデオ通話画面（相手あり）")
@Composable
private fun VideoCallUiPreviewWithRemote() {
    _0726risuTheme {
        VideoCallUi(
                statusText = "Connected",
                hasRemoteUser = true,
                onCallEnd = {},
                localSurfaceView = { Box(modifier=Modifier.fillMaxSize()){ Text("Local View") } },
                remoteSurfaceView = { Box(modifier=Modifier.fillMaxSize()){ Text("Remote View") } }
        )
    }
}

@Preview(showBackground = true, name = "ビデオ通話画面（相手なし）")
@Composable
private fun VideoCallUiPreviewWithoutRemote() {
    _0726risuTheme {
        VideoCallUi(
            statusText = "Waiting for others...",
            hasRemoteUser = false,
            onCallEnd = {},
            localSurfaceView = { Box(modifier=Modifier.fillMaxSize()){ Text("Local View") } },
            remoteSurfaceView = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Screen3Preview() {
    _0726risuTheme {
        var name by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }
        var selectedDays by remember { mutableStateOf<Set<String>>(setOf("月曜日", "金曜日")) }
        val daysOfWeek = listOf("日曜日", "月曜日", "火曜日", "水曜日", "木曜日", "金曜日", "土曜日")
        val scrollState = rememberScrollState()

        Scaffold(
            bottomBar = {
                NavigationBar(onHomeClick = {}, onNotificationsClick = {}, onSettingsClick = {})
            }
        ) { innerPadding ->
            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "通話相手の登録",
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                MyTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "通話相手の名前を入力してください。",
                    modifier = Modifier.width(320.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                MyTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = "通話相手と同じルーム ID\n を設定してください。",
                    modifier = Modifier.width(320.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                MyTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = "時間を入力してください。例:19:00",
                    modifier = Modifier.width(320.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                DayOfWeekSelector(
                    selectedDays = selectedDays,
                    onDaySelected = { day ->
                        selectedDays = if (selectedDays.contains(day)) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                    },
                    daysOfWeek = daysOfWeek,
                    modifier = Modifier.width(320.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                ActionButton(text = "完了", onClick = {})
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Screen4Preview() {
    _0726risuTheme {
        val sampleNotifications = listOf(
            NotificationInfo(title = "たまがけ さんとの通話時間です", message = "通話を開始しましょう。", timestamp = "2025/08/05 18:00"),
            NotificationInfo(title = "こうりん さんとの通話時間です", message = "通話を開始しましょう。", timestamp = "2025/08/05 19:00")
        )
        Scaffold(
            bottomBar = {
                NavigationBar(onHomeClick = {}, onNotificationsClick = {}, onSettingsClick = {})
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    "通知履歴",
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sampleNotifications) { info ->
                        NotificationItem(info = info)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Screen5Preview() {
    _0726risuTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(onHomeClick = {}, onNotificationsClick = {}, onSettingsClick = {})
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    "設定",
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CallInfoItemPreview() {
    _0726risuTheme {
        val sampleInfo = CallInfo(
            name = "プレビューユーザー",
            number = "000-0000-0000",
            time = "20:00",
            daysOfWeek = setOf(Calendar.TUESDAY, Calendar.THURSDAY, Calendar.SATURDAY)
        )
        CallInfoItem(
            info = sampleInfo,
            onVideoCall = {},
            onEdit = {},
            onDelete = {}
        )
    }
}