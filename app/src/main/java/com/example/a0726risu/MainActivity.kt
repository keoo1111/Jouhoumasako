package com.example.a0726risu

import android.Manifest
import android.app.AlarmManager
import android.app.Application
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
import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import android.view.SurfaceView
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import androidx.compose.runtime.LaunchedEffect

data class CallInfo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String,
    val time: String,
    val daysOfWeek: Set<Int>
)

class CallViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        application.getSharedPreferences("call_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _callList = MutableStateFlow<List<CallInfo>>(emptyList())
    val callList: StateFlow<List<CallInfo>> = _callList

    val notifications: StateFlow<List<NotificationInfo>> = NotificationRepository.notifications

    init {
        loadCallList()
    }

    fun addCallInfo(name: String, number: String, time: String, daysOfWeek: Set<Int>) {
        val newInfo = CallInfo(name = name, number = number, time = time, daysOfWeek = daysOfWeek)
        _callList.value += newInfo
        saveCallList()
    }

    fun deleteCallInfo(id: String) {
        _callList.value = _callList.value.filterNot { it.id == id }
        saveCallList()
    }

    fun getCallInfoById(id: String): CallInfo? {
        return _callList.value.find { it.id == id }
    }

    fun updateCallInfo(id: String, name: String, number: String, time: String, daysOfWeek: Set<Int>) {
        val updatedList = _callList.value.map { info ->
            if (info.id == id) {
                info.copy(name = name, number = number, time = time, daysOfWeek = daysOfWeek)
            } else {
                info
            }
        }
        _callList.value = updatedList
        saveCallList()
    }

    private fun saveCallList() {
        val jsonString = gson.toJson(_callList.value)
        sharedPreferences.edit().putString("call_list_key", jsonString).apply()
    }

    private fun loadCallList() {
        val jsonString = sharedPreferences.getString("call_list_key", null)
        if (jsonString != null) {
            val type = object : TypeToken<List<CallInfo>>() {}.type
            _callList.value = gson.fromJson(jsonString, type)
        }
    }
}

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
                                    Log.d("Screen1", "通話開始ボタンクリック。固定チャンネル [$channelName] に接続します。")
                                    navController.navigate("video_call/$channelName")
                                },
                                onDeleteCall = { id -> callViewModel.deleteCallInfo(id) }
                            )
                        }
                        composable(
                            route = "video_call/{channelName}",
                            arguments = listOf(navArgument("channelName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
                            VideoCallScreen(
                                channelName = channelName,
                                onNavigateBack = { navController.popBackStack() }
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
                                callViewModel = callViewModel, // ViewModel を渡す
                                onAddClick = { name, number, time, daysOfWeek ->
                                    callViewModel.addCallInfo(name, number, time, daysOfWeek)
                                    daysOfWeek.forEach { dayInt ->
                                        scheduleNotification(
                                            context = this@MainActivity,
                                            time = time,
                                            dayOfWeek = dayInt,
                                            title = "$name さんとの通話時間です"
                                        )
                                    }
                                    navController.popBackStack()
                                },
                                onUpdateClick = { id, name, number, time, daysOfWeek ->
                                    callViewModel.updateCallInfo(id, name, number, time, daysOfWeek)
                                    daysOfWeek.forEach { dayInt ->
                                        scheduleNotification(
                                            context = this@MainActivity,
                                            time = time,
                                            dayOfWeek = dayInt,
                                            title = "$name さんとの通話時間です"
                                        )
                                    }
                                    navController.popBackStack()
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
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
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

            val message = "${deniedPermissionNames.joinToString("、")} の権限が許可されていません。設定から許可してください。"
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

    //通知をスケジュールする
    private fun scheduleNotification(context: Context, time: String, dayOfWeek: Int, title: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, "通話を開始しましょう。")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            UUID.randomUUID().hashCode(),
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
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "時間の形式が正しくありません (HH:mm)", Toast.LENGTH_SHORT).show()
                    return
                }
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Toast.makeText(this, "通知を $time にセットしました", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "アラームの権限がありません。設定画面に移動します。", Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, "通知を $time にセットしました", Toast.LENGTH_LONG).show()
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
        val context = LocalContext.current

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
                        .padding(top = 8.dp, end = 16.dp)
                )
            }
        }
    }

private const val YOUR_APP_ID = "dd4ae31edd954871979236cff5e4c1bc"
private const val TAG = "VideoCallScreen"

@Composable
fun VideoCallScreen(
    channelName: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var remoteUid by remember { mutableStateOf<Int?>(null) }
    val rtcEngine = remember { mutableStateOf<RtcEngine?>(null) }
    var statusText by remember { mutableStateOf("Initializing...") }

    DisposableEffect(Unit) {
        var engine: RtcEngine? = null
        try {
            val eventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Log.d(TAG, "Successfully joined channel: $channel with uid: $uid")
                    statusText = "Connected. Waiting for others..."
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d(TAG, "Remote user joined: $uid")
                    statusText = "Remote user connected."
                    remoteUid = uid
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d(TAG, "Remote user offline: $uid, reason: $reason")
                    statusText = "Remote user left."
                    remoteUid = null
                }

                override fun onError(err: Int) {
                    Log.e(TAG, "Agora RTC Error: $err")
                    statusText = "Error occurred: $err"
                }
            }

            Log.d(TAG, "Initializing Agora RtcEngine...")
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = YOUR_APP_ID
            config.mEventHandler = eventHandler
            engine = RtcEngine.create(config)
            rtcEngine.value = engine

            engine.enableVideo()
            Log.d(TAG, "Video enabled.")

            engine.startPreview()
            Log.d(TAG, "Local preview started.")

            Log.d(TAG, "Joining channel: $channelName")
            val token = "007eJxTYFi3eOEHm/N3eVdvsfh/8mjECgYhhhX7FmV+NV6jrHGGe2aGAkNKikliqrFhakqKpamJhbmhpbmlkbFZclqaaapJsmFS8i2+lRkNgYwM+exSLIwMEAjiMzMYGRkxMAAAiIMesw=="
            val result = engine.joinChannel(token, channelName, null, 0)
            if (result != 0) {
                val errorMessage = "Failed to join channel. Error code: $result. Check your App ID, token, and channel name."
                Log.e(TAG, errorMessage)
                statusText = errorMessage
            } else {
                statusText = "Joining channel..."
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during Agora initialization", e)
            statusText = "Initialization failed: ${e.message}"
        }

        onDispose {
            Log.d(TAG, "Disposing VideoCallScreen. Leaving channel.")
            engine?.apply {
                stopPreview()
                leaveChannel()
            }
            RtcEngine.destroy()
            Log.d(TAG, "RtcEngine destroyed.")
        }
    }

    VideoCallUi(
        statusText = statusText,
        hasRemoteUser = remoteUid != null,
        onCallEnd = onNavigateBack,
        localSurfaceView = {
            AndroidView(
                factory = {
                    SurfaceView(it)
                },
                update = { view ->
                    rtcEngine.value?.setupLocalVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_FIT, 0))
                }
            )
        },
        remoteSurfaceView = {
            remoteUid?.let { uid ->
                AndroidView(
                    factory = { SurfaceView(it) },
                    update = { view ->
                        rtcEngine.value?.setupRemoteVideo(VideoCanvas(view, VideoCanvas.RENDER_MODE_FIT, uid))
                    }
                )
            }
        }
    )
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
        "日曜日" to Calendar.SUNDAY, "月曜日" to Calendar.MONDAY, "火曜日" to Calendar.TUESDAY,
        "水曜日" to Calendar.WEDNESDAY, "木曜日" to Calendar.THURSDAY, "金曜日" to Calendar.FRIDAY,
        "土曜日" to Calendar.SATURDAY
    )
    var selectedDays by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
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
                        val daysOfWeekInts = selectedDays.mapNotNull { daysOfWeekMap[it] }.toSet()
                        if (isEditing) {
                            onUpdateClick(callId, name, number, time, daysOfWeekInts)
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
                style = MaterialTheme.typography.headlineMedium)

        }
    }
}