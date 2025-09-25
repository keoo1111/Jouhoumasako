package com.example.a0726risu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.a0726risu.ui.theme._0726risuTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.view.SurfaceView
import androidx.compose.ui.viewinterop.AndroidView
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class VideoCallActivity : ComponentActivity() {
    // --- 変更・追加点 ---
    private var agoraToken by mutableStateOf<String?>(null)
    private var statusText by mutableStateOf("トークンを取得中...")
    private var hasRemoteUser by mutableStateOf(false) // 相手がいるかどうかを管理

    // Agora エンジンと映像表示用の View を保持する変数
    private var rtcEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    // 相手のユーザーID を保持する変数
    private var remoteUid: Int = 0
    // --- ここまで ---


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelName = intent.getStringExtra("CHANNEL_NAME") ?: "test"
        // Intent から直接 TOKEN を受け取るように修正
        agoraToken = intent.getStringExtra("TOKEN")

        // トークンが正常に渡されているかチェック
        if (agoraToken == null) {
            statusText = "エラー: トークンがありません。"
        } else {
            // Agora エンジンをセットアップ
            setupVideoSDKEngine(channelName, agoraToken!!)
        }

        setContent {
            _0726risuTheme {
                val allTopics = remember { TopicRepository.getAllTopics() }

                VideoCallUi(
                    statusText = statusText,
                    hasRemoteUser = hasRemoteUser, // ★ 状態変数を利用
                    onCallEnd = {
                        rtcEngine?.leaveChannel()
                        finish()
                    },
                    // ★ SurfaceView を UI に渡す
                    localSurfaceView = {
                        if (localSurfaceView != null) {
                            AndroidView(factory = { localSurfaceView!! })
                        }
                    },
                    remoteSurfaceView = {
                        if (remoteSurfaceView != null) {
                            AndroidView(factory = { remoteSurfaceView!! })
                        }
                    },
                    topics = allTopics
                )
            }
        }
    }

    private fun setupVideoSDKEngine(channelName: String, token: String) {
        try {
            val config = RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = AppConstants.AGORA_APP_ID
            config.mEventHandler = mRtcEventHandler
            rtcEngine = RtcEngine.create(config)

            rtcEngine?.enableVideo()

            // 自分の映像を表示する準備
            localSurfaceView = SurfaceView(baseContext)
            rtcEngine?.setupLocalVideo(VideoCanvas(localSurfaceView, Constants.RENDER_MODE_FIT, 0))

            // チャンネルに参加
            val options = ChannelMediaOptions()
            options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            rtcEngine?.joinChannel(token, channelName, 0, options)

        } catch (e: Exception) {
            e.printStackTrace()
            statusText = "エラー: 通話エンジンの初期化に失敗"
        }
    }

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // 自分がチャンネルに参加成功したときのコールバック
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            runOnUiThread {
                statusText = "相手の参加を待っています..."
            }
        }

        // 他のユーザーがチャンネルに参加したときのコールバック
        override fun onUserJoined(uid: Int, elapsed: Int) {
            remoteUid = uid
            runOnUiThread {
                hasRemoteUser = true
                statusText = "" // 相手が来たらステータスは消す
                // 相手の映像を表示する準備
                remoteSurfaceView = SurfaceView(baseContext)
                rtcEngine?.setupRemoteVideo(VideoCanvas(remoteSurfaceView, Constants.RENDER_MODE_FIT, uid))
            }
        }

        // 他のユーザーがチャンネルから退出したときのコールバック
        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                hasRemoteUser = false
                remoteSurfaceView = null
                statusText = "相手が退出しました"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
    }
    // --- ここまで追加 ---
}