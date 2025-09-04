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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.a0726risu.AppConstants.EXTRA_TOKEN

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        askPermissions()
        setContent {
            _0726risuTheme {
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
                            Screen1(
                                callList = callList,
                                navController = navController,
                                onNavigateToScreen3 = { navController.navigate("screen3") },
                                onNavigateToEditScreen = { callId ->
                                    navController.navigate("screen3?callId=$callId")
                                },
                                onNavigateToVideoCall = { channelName ->
                                    val intent = Intent(
                                        this@MainActivity,
                                        VideoCallActivity::class.java
                                    ).apply {
                                        putExtra("CHANNEL_NAME", channelName)
                                        putExtra("TOKEN", "007eJxTYHDes7IsYgdji1ozo/XdIzHORj1RZ/O29tnNv37cd1nr+7sKDCkpJompxoapKSmWpiYW5oaW5pZGxmbJaWmmqSbJhknJGi/WZzQEMjLUrDjHyMgAgSA+M4OhoSEDAwBgsh/1")
                                    }
                                    startActivity(intent)
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
                            Screen5(navController = navController)
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
            putExtra(EXTRA_TOKEN, AppConstants.AGORA_TOKEN)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    Text(
                        "リスト",
                        fontSize = 24.sp,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    if (callList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "通話相手が登録されていません。\n右上の「＋」ボタンから登録してください。",
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
                ActionButton(
                    text = "＋",
                    onClick = onNavigateToScreen3,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(innerPadding)
                        .padding(top = 8.dp, end = 16.dp)
                )
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

    @Composable//設定画面
    fun Screen5(navController: NavController, modifier: Modifier = Modifier) {
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
