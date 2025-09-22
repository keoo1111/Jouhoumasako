package com.example.a0726risu.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.a0726risu.FontSize

// --- 元のコードから持ってきた部分 ---
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun rememberScaledTypography(fontSize: FontSize): Typography {
    val default = MaterialTheme.typography
    val scale = fontSize.scale
    return remember(scale) {
        Typography(
            displayLarge = default.displayLarge.copy(fontSize = default.displayLarge.fontSize * scale),
            displayMedium = default.displayMedium.copy(fontSize = default.displayMedium.fontSize * scale),
            displaySmall = default.displaySmall.copy(fontSize = default.displaySmall.fontSize * scale),
            headlineLarge = default.headlineLarge.copy(fontSize = default.headlineLarge.fontSize * scale),
            headlineMedium = default.headlineMedium.copy(fontSize = default.headlineMedium.fontSize * scale),
            headlineSmall = default.headlineSmall.copy(fontSize = default.headlineSmall.fontSize * scale),
            titleLarge = default.titleLarge.copy(fontSize = default.titleLarge.fontSize * scale),
            titleMedium = default.titleMedium.copy(fontSize = default.titleMedium.fontSize * scale),
            titleSmall = default.titleSmall.copy(fontSize = default.titleSmall.fontSize * scale),
            bodyLarge = default.bodyLarge.copy(fontSize = default.bodyLarge.fontSize * scale),
            bodyMedium = default.bodyMedium.copy(fontSize = default.bodyMedium.fontSize * scale),
            bodySmall = default.bodySmall.copy(fontSize = default.bodySmall.fontSize * scale),
            labelLarge = default.labelLarge.copy(fontSize = default.labelLarge.fontSize * scale),
            labelMedium = default.labelMedium.copy(fontSize = default.labelMedium.fontSize * scale),
            labelSmall = default.labelSmall.copy(fontSize = default.labelSmall.fontSize * scale)
        )
    }
}


@Composable
fun _0726risuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    fontSize: FontSize = FontSize.MEDIUM, // ★ 引数を追加
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // ★ ステータスバーの色を調整する処理を追加
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    val typography = rememberScaledTypography(fontSize = fontSize)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography, // ★ ここに適用
        content = content
    )
}
