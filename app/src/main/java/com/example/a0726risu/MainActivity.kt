package com.example.a0726risu

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.a0726risu.ui.theme._0726risuTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.a0726risu.AppConstants.CHANNEL_ID
import com.example.a0726risu.AppConstants.EXTRA_CHANNEL_NAME
import com.example.a0726risu.AppConstants.EXTRA_DAY_OF_WEEK
import com.example.a0726risu.AppConstants.EXTRA_MESSAGE
import com.example.a0726risu.AppConstants.EXTRA_TIME
import com.example.a0726risu.AppConstants.EXTRA_TITLE
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

object DailyTrivia {
    private val triviaMap = mapOf(
        // 1 月
        "0101" to "あけましておめでとうございます。元旦ですね。",
        "0107" to "今日は七草がゆを食べる日ですね。無病息災を祈ります。",
        "0110" to "今日は「110 番の日」です。いざという時のために覚えておきましょう。",
        "0115" to "「いちごの日」だそうです。美味しいいちごが食べたくなりますね。",

        // 2 月
        "0203" to "今日は節分です。豆まきで鬼を追い払いましょう。",
        "0211" to "建国記念の日ですね。",
        "0214" to "バレンタインデーですね。チョコレートが美味しい季節です。",
        "0222" to "にゃん・にゃん・にゃんで「猫の日」だそうです。可愛いですね。",

        // 3 月
        "0303" to "ひな祭りの日ですね。ちらし寿司やはまぐりのお吸い物が美味しいです。",
        "0314" to "ホワイトデーですね。お返しのプレゼントを選ぶのも楽しいです。",
        "0320" to "春分の日です。だんだんと昼が長くなり、春の訪れを感じますね。",

        // 4 月
        "0401" to "今日から新年度の始まりですね。何か新しいことを始めるのもいいですね。",
        "0408" to "お釈迦様の誕生日「花まつり」の日です。",
        "0429" to "昭和の日ですね。懐かしい時代に思いを馳せる日です。",

        // 5 月
        "0505" to "こどもの日です。柏餅を食べる日ですね。",
        "0508" to "5(ご)8(よう) で「ゴーヤーの日」だそうです。沖縄の夏野菜ですね。",
        "0509" to "今日は母の日。いつもありがとう、と感謝を伝える日ですね。",

        // 6 月
        "0606" to "梅雨の季節ですね。綺麗なアジサイの花が見頃になります。",
        "0610" to "時の記念日です。時間を大切にしたいですね。",
        "0615" to "今日は「オウムとインコの日」だそうですよ。",


        // 7 月
        "0707" to "七夕ですね。短冊にお願い事は書きましたか？",



        // 8 月
        "0807" to "「バナナの日」だそうです。手軽に栄養がとれますね。",
        "0811" to "山の日です。山の恵みに感謝する日ですね。",
        "0813" to "お盆の時期ですね。ご先祖様をお迎えします。",
        "0815" to "終戦記念日です。平和を願う日ですね。",
        "0831" to "8(や)3(さ)1(い) で「野菜の日」だそうです。たくさん食べたいですね。",

        // 9 月
        "0901" to "防災の日です。いざという時の備えを確認しておくと安心ですね。",
        "0909" to "菊の節句（重陽の節句）です。菊の花を飾ったり、栗ご飯を食べたりします。",

        "0923" to "秋分の日です。これからだんだんと秋が深まっていきます。",

        // 10 月
        "1001" to "今日は「コーヒーの日」。温かいコーヒーで一息つきませんか？",
        "1010" to "昔の体育の日ですね。体を動かすのに気持ちの良い季節です。",
        "1013" to "「さつまいもの日」だそうです。焼き芋が美味しい季節ですね。",
        "1031" to "ハロウィンですね。かぼちゃの季節です。",

        // 11 月
        "1103" to "文化の日です。芸術の秋、美術館などにお出かけもいいですね。",
        "1115" to "七五三の日です。子どもの健やかな成長をお祝いする日ですね。",
        "1122" to "「いい夫婦の日」ですね。大切な人と過ごすのも素敵ですね。",
        "1123" to "勤労感謝の日です。いつもお仕事お疲れ様です。",

        // 12 月
        "1224" to "クリスマスイブですね。素敵な一日をお過ごしください。",
        "1225" to "クリスマスです。街がキラキラして綺麗ですね。",
        "1231" to "大晦日です。一年間、本当にお疲れ様でした。"
    )

    fun getTriviaForToday(): String {
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("MMdd", Locale.getDefault())
        val dateKey = format.format(calendar.time)
        return triviaMap[dateKey] ?: "今日も素敵な一日になりますように。"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        askPermissions()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val fontSize by settingsViewModel.fontSize.collectAsState()
            _0726risuTheme(fontSize = fontSize) {

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val callViewModel: CallViewModel = viewModel()

                    NavHost(navController = navController, startDestination = "screen1") {
                        composable("screen1") {
                            val callList by callViewModel.callList.collectAsState()
                            LaunchedEffect(Unit) {
                                callViewModel.uiEvent.collect { event ->
                                    when (event) {
                                        is CallViewModel.UiEvent.NavigateToVideoCall -> {
                                            val intent = Intent(this@MainActivity, VideoCallActivity::class.java).apply {
                                                putExtra("CHANNEL_NAME", event.channelName)
                                                putExtra("TOKEN", event.token) // ★サーバーから取得したトークンを渡す
                                            }
                                            startActivity(intent)
                                        }
                                        is CallViewModel.UiEvent.ShowError -> {
                                            Toast.makeText(this@MainActivity, event.message, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                            Screen1(
                                callList = callList,
                                navController = navController,
                                onNavigateToScreen3 = { navController.navigate("screen3") },
                                onNavigateToEditScreen = { callId ->
                                    navController.navigate("screen3?callId=$callId")
                                },
                                onNavigateToVideoCall = { channelName ->
                                    callViewModel.fetchTokenAndNavigate(channelName)
                                },
                                onDeleteCall = { id -> callViewModel.deleteCallInfo(id) }
                            )
                        }
                        composable(
                            route = "screen3?callId={callId}",
                            arguments = listOf(navArgument("callId") {
                                type = NavType.StringType
                                nullable = true
                            })
                        ) { backStackEntry ->
                            val callId = backStackEntry.arguments?.getString("callId")
                            Screen3(
                                callId = callId,
                                callViewModel = callViewModel,
                                onAddClick = { name, number, time, daysOfWeek ->
                                    lifecycleScope.launch {
                                        withContext(Dispatchers.IO) {
                                            callViewModel.addCallInfo(
                                                name,
                                                number,
                                                time,
                                                daysOfWeek
                                            )
                                            daysOfWeek.forEach { dayInt ->
                                                scheduleAutoCall(
                                                    context = this@MainActivity,
                                                    time = time,
                                                    dayOfWeek = dayInt,
                                                    title = "$name さんとの通話時間です",
                                                    channelName = number
                                                )
                                            }
                                        }
                                        navController.popBackStack()
                                    }
                                },
                                onUpdateClick = { id, name, number, time, daysOfWeek ->
                                    lifecycleScope.launch {
                                        withContext(Dispatchers.IO) {
                                            val oldCallInfo = callViewModel.getCallInfoById(id)
                                            if (oldCallInfo != null) {
                                                cancelAlarmsForCall(oldCallInfo)
                                            }
                                            callViewModel.updateCallInfo(
                                                id,
                                                name,
                                                number,
                                                time,
                                                daysOfWeek
                                            )
                                            daysOfWeek.forEach { dayInt ->
                                                scheduleAutoCall(
                                                    context = this@MainActivity,
                                                    time = time,
                                                    dayOfWeek = dayInt,
                                                    title = "$name さんとの通話時間です",
                                                    channelName = number
                                                )
                                            }
                                        }
                                        navController.popBackStack()
                                    }
                                },
                                navController = navController
                            )
                        }
                        composable("screen4") {
                            val notifications by callViewModel.notifications.collectAsState()
                            Screen4(
                                navController = navController,
                                notifications = notifications
                            )
                        }
                        composable("screen5") {
                            Screen5(
                                navController = navController,
                                settingsViewModel = settingsViewModel)
                        }
                    }
                }
            }
        }
    }


    //通知、カメラ、マイクの権限を要求する
    private fun askPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        val permissions = listOfNotNull(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.entries.filter { !it.value }

        if (deniedPermissions.isNotEmpty()) {
            val deniedPermissionNames = deniedPermissions.map {
                when (it.key) {
                    Manifest.permission.POST_NOTIFICATIONS -> "通知"
                    Manifest.permission.CAMERA -> "カメラ"
                    Manifest.permission.RECORD_AUDIO -> "マイク"
                    else -> "不明な権限"
                }
            }

            val message =
                "${deniedPermissionNames.joinToString("、")} の権限が許可されていません。設定から許可してください。"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }


    //通知チャンネルを作成する
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "通話リマインダー"
            val descriptionText = "設定した時間に通話を通知します"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun cancelAlarmsForCall(callInfo: CallInfo) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)

        callInfo.daysOfWeek.forEach { dayOfWeek ->
            val requestCode = (callInfo.number + dayOfWeek + callInfo.time).hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "${callInfo.time}の古い予約をキャンセルしました",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    //通知をスケジュールする
    private fun scheduleAutoCall(
        context: Context,
        time: String,
        dayOfWeek: Int,
        title: String,
        channelName: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, "時間になりました。通話を開始します。")
            putExtra(EXTRA_CHANNEL_NAME, channelName)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek)
        }

        val requestCode = (channelName + dayOfWeek + time).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            try {
                val date = sdf.parse(time)
                date?.let {
                    val cal = Calendar.getInstance()
                    cal.time = it
                    set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, cal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "時間の形式が正しくありません (HH:mm)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "自動通話を $time にセットしました", Toast.LENGTH_LONG)
                        .show()
                }
            } else {
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,"アラームの権限がありません。設定画面に移動します。",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Intent().also {
                    it.action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    it.data = Uri.fromParts("package", packageName, null)
                    startActivity(it)
                }
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            lifecycleScope.launch(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "自動通話を $time にセットしました", Toast.LENGTH_LONG).show()
            }
        }
    }

    @Composable//ホーム画面
    fun Screen1(
        callList: List<CallInfo>,
        navController: NavController,
        modifier: Modifier = Modifier,
        onNavigateToScreen3: () -> Unit,
        onNavigateToEditScreen: (String) -> Unit,
        onNavigateToVideoCall: (String) -> Unit,
        onDeleteCall: (String) -> Unit,
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    onHomeClick = {
                        navController.navigate("screen1") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNotificationsClick = { navController.navigate("screen4") },
                    onSettingsClick = { navController.navigate("screen5") }
                )
            }
        ) { innerPadding ->
            // ===== 変更 =====
            // ActionButton を Column 内に移動させるため、Box はシンプルなコンテナとして機能します。
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "今日のひとこと",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = DailyTrivia.getTriviaForToday(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // ===== 変更点 ここから =====
                // Text と ActionButton を Row で囲み、横一列に配置します
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp), // この行の下にリストとの余白を設けます
                    horizontalArrangement = Arrangement.SpaceBetween, // 要素を両端に配置します
                    verticalAlignment = Alignment.CenterVertically // 要素を垂直方向の中央に揃えます
                ) {
                    // 「リスト」テキスト
                    Text(
                        text = "リスト",
                        fontSize = 24.sp,
                        style = MaterialTheme.typography.titleLarge
                        // ここにあった Modifier.padding(bottom = 16.dp) は親の Row に移動しました
                    )
                    // 「＋ 通話相手を追加」ボタン
                    ActionButton(
                        text = "＋ 通話相手を追加",
                        onClick = onNavigateToScreen3
                        // ここにあった Modifier は Row が管理するため不要になりました
                    )
                }
                // ===== 変更点 ここまで =====
                if (callList.isEmpty()) {
                    Box(
                        // ===== 変更 =====
                        // Column が親になったため、残りのスペースを埋めるように weight を使用
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "通話相手が登録されていません。\n上の「＋ 通話相手を追加」ボタンから登録してください。",
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(callList, key = { it.id }) { info ->
                            CallInfoItem(
                                info = info,
                                onVideoCall = { onNavigateToVideoCall(info.number) },
                                onEdit = { onNavigateToEditScreen(info.id) },
                                onDelete = { onDeleteCall(info.id) }
                            )
                        }
                    }
                }
            }


            }
        }
    }

    @Composable//登録画面
    fun Screen3(
        callId: String?,
        onAddClick: (name: String, number: String, time: String, daysOfWeek: Set<Int>) -> Unit,
        onUpdateClick: (id: String, name: String, number: String, time: String, daysOfWeek: Set<Int>) -> Unit,
        navController: NavController,
        modifier: Modifier = Modifier,
        callViewModel: CallViewModel
    ) {
        val isEditing = callId != null
        var name by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }

        val daysOfWeekMap = mapOf(
            "日曜日" to Calendar.SUNDAY,
            "月曜日" to Calendar.MONDAY,
            "火曜日" to Calendar.TUESDAY,
            "水曜日" to Calendar.WEDNESDAY,
            "木曜日" to Calendar.THURSDAY,
            "金曜日" to Calendar.FRIDAY,
            "土曜日" to Calendar.SATURDAY
        )
        var selectedDays by remember { mutableStateOf<Set<String>>(emptySet()) }
        val scrollState = rememberScrollState()

        LaunchedEffect(key1 = callId) {
            if (isEditing) {
                callViewModel.getCallInfoById(callId!!)?.let { info ->
                    name = info.name
                    number = info.number
                    time = info.time
                    selectedDays = info.daysOfWeek.mapNotNull { dayInt ->
                        daysOfWeekMap.entries.find { it.value == dayInt }?.key
                    }.toSet()
                }
            } else {
                name = ""
                number = ""
                time = ""
                selectedDays = emptySet()
            }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    onHomeClick = {
                        navController.navigate("screen1") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNotificationsClick = { navController.navigate("screen4") },
                    onSettingsClick = { navController.navigate("screen5") }
                )
            }
        ) { innerPadding ->
            Column(
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
                    text = if (isEditing) "通話相手の編集" else "通話相手の登録",
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
                    label = "通話相手と同じルーム ID を\n設定してください。",
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
                    daysOfWeek = daysOfWeekMap.keys.toList(),
                    modifier = Modifier.width(320.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                ActionButton(
                    text = if (isEditing) "更新" else "完了",
                    onClick = {
                        if (name.isNotBlank() && number.isNotBlank() && time.isNotBlank() && selectedDays.isNotEmpty()) {
                            val daysOfWeekInts =
                                selectedDays.mapNotNull { daysOfWeekMap[it] }.toSet()
                            if (isEditing) {
                                onUpdateClick(callId!!, name, number, time, daysOfWeekInts)
                            } else {
                                onAddClick(name, number, time, daysOfWeekInts)
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable//通知画面
    fun Screen4(
        modifier: Modifier = Modifier,
        navController: NavController,
        notifications: List<NotificationInfo>
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    onHomeClick = {
                        navController.navigate("screen1") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNotificationsClick = { navController.navigate("screen4") },
                    onSettingsClick = { navController.navigate("screen5") }
                )
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
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "通知履歴はありません。",
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { info ->
                            NotificationItem(info = info)
                        }
                    }
                }
            }
        }
    }

@Composable
fun Screen5(
    navController: NavController,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel
) {
        val currentFontSize by settingsViewModel.fontSize.collectAsState()

        Scaffold(
            bottomBar = {
                NavigationBar(
                    onHomeClick = {
                        navController.navigate("screen1") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNotificationsClick = { navController.navigate("screen4") },
                    onSettingsClick = { navController.navigate("screen5") }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            ) {
                Text(
                    "設定",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 文字サイズ設定 UI
                Text(
                    text = "文字サイズ",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FontSize.values().forEach { fontSize ->
                        Button(
                            onClick = { settingsViewModel.setFontSize(fontSize) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentFontSize == fontSize) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (currentFontSize == fontSize) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(fontSize.displayName)
                        }
                    }
                }
            }
        }
    }
