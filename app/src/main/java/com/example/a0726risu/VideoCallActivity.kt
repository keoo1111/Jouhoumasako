package com.example.a0726risu

import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.a0726risu.ui.theme._0726risuTheme
import io.agora.rtc2.DataStreamConfig
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlin.text.Charsets

private const val TAG_VC = "VideoCallActivity"

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationRepository.init(this)
    }
}

class VideoCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val channelName = intent.getStringExtra("CHANNEL_NAME") ?: ""
        val token = intent.getStringExtra("TOKEN") ?: ""
        setContent {
            _0726risuTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VideoCallScreenContent(
                        channelName = channelName,
                        token = token,
                        onNavigateBack = { finishAndRemoveTask() }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoCallScreenContent(
    channelName: String,
    token: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var remoteUid by remember { mutableStateOf<Int?>(null) }
    val rtcEngine = remember { mutableStateOf<RtcEngine?>(null) }
    var statusText by remember { mutableStateOf("初期化中...") }
    var currentTopic by remember { mutableStateOf("ボタンを押して話題を変えよう！") }
    val streamId = remember { mutableStateOf<Int?>(null) }
    var currentTopicIndex by remember { mutableStateOf(-1) }

    DisposableEffect(Unit) {
        var engine: RtcEngine? = null
        try {
            val eventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Log.d(TAG_VC, "Successfully joined channel: $channel with uid: $uid")
                    statusText = "相手を待っています..."
                }
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d(TAG_VC, "Remote user joined: $uid")
                    statusText = "相手が入室しました。"
                    remoteUid = uid
                    if (currentTopicIndex != -1) {
                        streamId.value?.let { id ->
                            val messageData = currentTopicIndex.toString().toByteArray(Charsets.UTF_8)
                            engine?.sendStreamMessage(id, messageData)
                        }
                    }
                }
                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d(TAG_VC, "Remote user offline: $uid, reason: $reason")
                    statusText = "相手が切断しました。"
                    remoteUid = null
                }
                override fun onError(err: Int) {
                    Log.e(TAG_VC, "Agora RTC Error: $err")
                    statusText = "エラーが発生しました：$err"
                }
                override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray) {
                    val receivedIndex = data.toString(Charsets.UTF_8).toIntOrNull()
                    if (receivedIndex != null) {
                        currentTopicIndex = receivedIndex
                        currentTopic = TopicRepository.getTopicByIndex(receivedIndex)
                    }
                }
                override fun onStreamMessageError(uid: Int, streamId: Int, error: Int, missed: Int, cached: Int) {
                    Log.e(TAG_VC, "Stream message error from $uid: $error")
                }
            }
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = AppConstants.AGORA_APP_ID
            config.mEventHandler = eventHandler
            engine = RtcEngine.create(config)
            rtcEngine.value = engine
            val streamConfig = DataStreamConfig()
            streamConfig.syncWithAudio = false
            streamConfig.ordered = true
            val id = engine.createDataStream(streamConfig)
            streamId.value = id
            engine.enableVideo()
            engine.startPreview()
            val result = engine.joinChannel(token, channelName, null, 0)
            if (result != 0) {
                statusText = "チャンネル参加に失敗しました。コード：$result"
            } else {
                statusText = "チャンネルに参加中..."
            }
        } catch (e: Exception) {
            statusText = "初期化に失敗しました：${e.message}"
        }
        onDispose {
            engine?.apply {
                streamId.value?.let { RtcEngine.destroy() }
                stopPreview()
                leaveChannel()
            }
            RtcEngine.destroy()
        }
    }

    VideoCallUi(
        statusText = statusText,
        hasRemoteUser = remoteUid != null,
        currentTopic = currentTopic,
        onNextTopicClick = {
            streamId.value?.let { id ->
                val newTopicIndex = (0 until TopicRepository.getTopicCount()).random()
                currentTopicIndex = newTopicIndex
                currentTopic = TopicRepository.getTopicByIndex(newTopicIndex)
                val messageData = newTopicIndex.toString().toByteArray(Charsets.UTF_8)
                rtcEngine.value?.sendStreamMessage(id, messageData)
            }
        },
        onCallEnd = onNavigateBack,
        localSurfaceView = {
            AndroidView(
                factory = { SurfaceView(it) },
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

@Composable
fun VideoCallUi(
    statusText: String,
    hasRemoteUser: Boolean,
    currentTopic: String,
    onCallEnd: () -> Unit,
    onNextTopicClick: () -> Unit,
    localSurfaceView: @Composable () -> Unit,
    remoteSurfaceView: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasRemoteUser) {
            Box(modifier = Modifier.fillMaxSize()) { remoteSurfaceView() }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(statusText, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .width(120.dp)
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            localSurfaceView()
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTopic,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onNextTopicClick,
                shape = RoundedCornerShape(50)
            ) {
                Text("次の話題", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onCallEnd,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("通話を終了する", fontSize = 14.sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "ビデオ通話画面（相手あり）")
@Composable
private fun VideoCallUiPreviewWithRemote() {
    _0726risuTheme {
        VideoCallUi(
            statusText = "通話に接続しました",
            hasRemoteUser = true,
            currentTopic = "最近ハマっていることは何？",
            onNextTopicClick = {},
            onCallEnd = {},
            localSurfaceView = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("自分の映像")
                }
            },
            remoteSurfaceView = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("相手の映像")
                }
            }
        )
    }
}

@Preview(showBackground = true, name = "ビデオ通話画面（相手なし）")
@Composable
private fun VideoCallUiPreviewWithoutRemote() {
    _0726risuTheme {
        VideoCallUi(
            statusText = "相手を待っています...",
            hasRemoteUser = false,
            currentTopic = "ボタンを押して話題を変えよう！",
            onNextTopicClick = {},
            onCallEnd = {},
            localSurfaceView = {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("自分の映像")
                }
            },
            remoteSurfaceView = {}
        )
    }
}
