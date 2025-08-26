package com.example.a0726risu

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.a0726risu.ui.theme._0726risuTheme
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

private const val YOUR_APP_ID_VC = "dd4ae31edd954871979236cff5e4c1bc"
private const val TAG_VC = "VideoCallActivity"

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
    var statusText by remember { mutableStateOf("Initializing...") }

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
            }

            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = AppConstants.AGORA_APP_ID
            config.mEventHandler = eventHandler
            engine = RtcEngine.create(config)
            rtcEngine.value = engine

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
                stopPreview()
                leaveChannel()
            }
            RtcEngine.destroy()
        }
    }

    VideoCallUi(
        statusText = statusText,
        hasRemoteUser = remoteUid != null,
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