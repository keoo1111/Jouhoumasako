package com.example.a0726risu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.a0726risu.ui.theme._0726risuTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VideoCallActivity : ComponentActivity() {
    private var agoraToken by mutableStateOf<String?>(null)
    private var statusText by mutableStateOf("トークンを取得中...")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val channelName = intent.getStringExtra("CHANNEL_NAME") ?: "test"
        lifecycleScope.launch {
            try {
                val response = AgoraApi.service.getAgoraToken(channelName)
                agoraToken = response.token
                statusText = "相手の参加を待っています..."
            } catch (e: Exception) {
                e.printStackTrace()
                statusText = "エラー: トークンの取得に失敗しました。"
            }
        }

        setContent {
            _0726risuTheme {
                val allTopics = remember { TopicRepository.getAllTopics() }

                VideoCallUi(
                    statusText = "相手の参加を待っています...",
                    hasRemoteUser = false, // 相手がいるかどうか (今は仮で false)
                    onCallEnd = { finish() }, // 終了ボタンでアクティビティを閉じる
                    localSurfaceView = { /* ローカル映像の View */ },
                    remoteSurfaceView = { /* リモート映像の View */ },
                    topics = allTopics // ★★★ 変更点 ★★★ 取得した全リストを渡す
                )
            }
        }
    }
}