package com.mathsquest.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Palette from the clickable prototype. Fills under white text are dark enough for 3:1 at 20sp+ bold. */
object MQ {
    val Ink = Color(0xFF14234B)
    val Muted = Color(0xFF4A5A80)
    val SkyTop = Color(0xFF1E7FE0)
    val SkyMid = Color(0xFF49A6F4)
    val SkyLow = Color(0xFFCDEBFF)
    val SkyBottom = Color(0xFFEEF8FF)
    val Line = Color(0xFFDCE9F8)
    val Tint = Color(0xFFF5FAFF)
    val TintBlue = Color(0xFFEAF3FF)
    val Blue = Color(0xFF1F74E0)
    val Green = Color(0xFF1C9A44)
    val Purple = Color(0xFF6A3FD8)
    val Orange = Color(0xFFD9560B)
    val DeepOrange = Color(0xFFC9500A)
    val Red = Color(0xFFD93636)
    val Pink = Color(0xFFC42E6E)
    val Teal = Color(0xFF0B8080)
    val Indigo = Color(0xFF3E46C9)
    val Gold = Color(0xFFFFC21A)
    val GoldDark = Color(0xFFC98A00)
    val GoldText = Color(0xFF6B4300)
    val GoldTint = Color(0xFFFFF1BF)
    val GreenTint = Color(0xFFE3F7EA)
    val GreenText = Color(0xFF135E2C)
    val RedTint = Color(0xFFFDE9E9)
    val RedText = Color(0xFF9B1C1C)
}

private val typography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp, color = MQ.Ink),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, color = MQ.Ink),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, color = MQ.Ink),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, color = MQ.Ink),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp, color = MQ.Ink),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, color = MQ.Ink),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, color = MQ.Ink),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 18.sp, color = MQ.Muted),
)

@Composable
fun MathsQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = MQ.Blue,
            onPrimary = Color.White,
            secondary = MQ.Green,
            background = MQ.SkyBottom,
            surface = Color.White,
            onSurface = MQ.Ink,
            error = MQ.Red,
        ),
        typography = typography,
        content = content,
    )
}
