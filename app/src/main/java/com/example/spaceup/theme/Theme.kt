package com.example.spaceup.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SpacePrimary,
    secondary = SpaceSecondary,
    tertiary = SpaceAccent,
    background = SpaceBackground,
    surface = SpaceCardBackground,
    onPrimary = SpaceTextPrimary,
    onSecondary = SpaceTextPrimary,
    onTertiary = SpaceTextPrimary,
    onBackground = SpaceTextPrimary,
    onSurface = SpaceTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = SpaceBackground, // Giữ nền tối cho cảm giác phi thuyền không gian
    surface = SpaceCardBackground,
    onPrimary = SpaceTextPrimary,
    onBackground = SpaceTextPrimary,
    onSurface = SpaceTextPrimary
)

@Composable
fun SpaceUpTheme(
  darkTheme: Boolean = true, // Ưu tiên giao diện tối để làm nổi bật chủ đề không gian
  dynamicColor: Boolean = false, // Vô hiệu hóa dynamic color để giữ màu Neon Space đồng nhất
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

