package com.example.a0726risu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

// このファイルには VideoCallActivity クラスだけを記述します
class VideoCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent からチャンネル名とトークンを受け取る
        val channelName = intent.getStringExtra("CHANNEL_NAME")
        val token = intent.getStringExtra("TOKEN")

        setContent {
            // ここに、以前あなたが作成した VideoCallUi Composable を呼び出すロジックなどを実装します
            // まずはコンパイルを通すために、簡単な UI を配置します
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "通話画面\nチャンネル名：$channelName")

                Button(
                    onClick = { finish() }, // ひとまず終了ボタンを設置
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Text("通話を終了")
                }
            }
        }
    }
}