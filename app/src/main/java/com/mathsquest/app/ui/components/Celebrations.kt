package com.mathsquest.app.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import com.mathsquest.app.ui.theme.MQ
import kotlin.random.Random

private val CONFETTI_COLOURS = listOf(
    MQ.Gold, MQ.Green, MQ.Blue, MQ.Pink, MQ.Purple, MQ.Orange, Color(0xFF3DDC6E), Color(0xFFFFE27A),
)

private class Piece(
    val x: Float, val vx: Float, val vy: Float, val spin: Float, val turns: Float,
    val w: Float, val h: Float, val colour: Color, val coin: Boolean, val delay: Float,
)

/** True unless the device has animations switched off (accessibility "remove animations"). */
@Composable
fun animationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
}

/**
 * A burst of confetti and gold coins that falls across its bounds once each time [trigger] changes.
 * Draws nothing when animations are off.
 */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier.fillMaxSize(), pieces: Int = 70, coins: Boolean = true) {
    if (trigger <= 0 || !animationsEnabled()) return
    val progress = remember(trigger) { Animatable(0f) }
    val parts = remember(trigger) {
        val r = Random(trigger * 7919)
        List(pieces) {
            val isCoin = coins && r.nextFloat() < 0.22f
            Piece(
                x = 0.5f + (r.nextFloat() - 0.5f) * 0.3f,
                vx = (r.nextFloat() - 0.5f) * 1.3f,
                vy = -(0.55f + r.nextFloat() * 0.6f),
                spin = r.nextFloat() * 360f,
                turns = (r.nextFloat() - 0.5f) * 1440f,
                w = if (isCoin) 22f else 10f + r.nextFloat() * 8f,
                h = if (isCoin) 22f else 16f + r.nextFloat() * 10f,
                colour = CONFETTI_COLOURS[r.nextInt(CONFETTI_COLOURS.size)],
                coin = isCoin,
                delay = r.nextFloat() * 0.15f,
            )
        }
    }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1_900, easing = LinearEasing))
    }
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val gravity = 2.2f
        parts.forEach { piece ->
            val t = ((p - piece.delay) / (1f - piece.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val x = (piece.x + piece.vx * t) * size.width
            val y = size.height * (0.42f + piece.vy * t + gravity * t * t * 0.5f)
            val alpha = if (t > 0.75f) (1f - t) / 0.25f else 1f
            if (piece.coin) {
                val r = piece.w / 2f
                drawCircle(MQ.GoldDark.copy(alpha = alpha), r, Offset(x, y))
                drawCircle(MQ.Gold.copy(alpha = alpha), r * 0.78f, Offset(x, y))
            } else {
                rotate(piece.spin + piece.turns * t, Offset(x, y)) {
                    drawRect(piece.colour.copy(alpha = alpha), Offset(x - piece.w / 2, y - piece.h / 2), Size(piece.w, piece.h))
                }
            }
        }
    }
}

/** Speaker icon drawn in code (the core icon set has no volume icons). A slash marks "off". */
@Composable
fun SpeakerIcon(on: Boolean, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.12f, h * 0.38f)
            lineTo(w * 0.32f, h * 0.38f)
            lineTo(w * 0.55f, h * 0.16f)
            lineTo(w * 0.55f, h * 0.84f)
            lineTo(w * 0.32f, h * 0.62f)
            lineTo(w * 0.12f, h * 0.62f)
            close()
        }
        drawPath(body, tint)
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        if (on) {
            drawArc(tint, -45f, 90f, false, Offset(w * 0.48f, h * 0.3f), Size(w * 0.3f, h * 0.4f), style = stroke)
            drawArc(tint, -50f, 100f, false, Offset(w * 0.46f, h * 0.15f), Size(w * 0.48f, h * 0.7f), style = stroke)
        } else {
            drawLine(tint, Offset(w * 0.66f, h * 0.36f), Offset(w * 0.9f, h * 0.64f), strokeWidth = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(tint, Offset(w * 0.9f, h * 0.36f), Offset(w * 0.66f, h * 0.64f), strokeWidth = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

/**
 * Content that flies in from off-screen ([fromX], [fromY] are directions: -1 left/top, 1 right/bottom),
 * lands with a squash-and-bounce after [delayMillis], and calls [onLand] as it touches down.
 * Replays whenever [key] changes. Shown in place immediately when animations are off.
 */
@Composable
fun FlyIn(
    key: Any,
    fromX: Float,
    fromY: Float,
    delayMillis: Int,
    modifier: Modifier = Modifier,
    onLand: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val enabled = animationsEnabled()
    val progress = remember(key) { Animatable(if (enabled) 0f else 1f) }
    val squash = remember(key) { Animatable(1f) }
    val land by rememberUpdatedState(onLand)
    LaunchedEffect(key) {
        if (!enabled) return@LaunchedEffect
        delay(delayMillis.toLong())
        // Speeds up as it falls, like a dropped coin.
        progress.animateTo(1f, tween(durationMillis = 430, easing = FastOutLinearInEasing))
        land()
        squash.snapTo(1.3f)
        squash.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMediumLow))
    }
    val distance = with(LocalDensity.current) { 460.dp.toPx() }
    Box(
        modifier.graphicsLayer {
            val p = progress.value
            translationX = fromX * distance * (1f - p)
            translationY = fromY * distance * (1f - p)
            rotationZ = (fromX * 40f - fromY * 25f) * (1f - p)
            alpha = if (p <= 0f) 0f else 1f
            scaleX = squash.value
            scaleY = 2f - squash.value
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}
