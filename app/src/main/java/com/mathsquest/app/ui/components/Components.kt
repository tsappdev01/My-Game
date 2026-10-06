package com.mathsquest.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathsquest.app.ui.theme.MQ
import kotlin.math.cos
import kotlin.math.sin

/** Blue sky background used behind every screen; content scrolls when it is taller than the screen. */
@Composable
fun SkyScreen(
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to MQ.SkyTop, 0.32f to MQ.SkyMid, 0.72f to MQ.SkyLow, 1f to MQ.SkyBottom)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    spacing: Dp = 12.dp,
    /** False lets children (such as flying numbers) draw outside the card while they move. */
    clipContent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, clip = clipContent, ambientColor = Color(0x2E0A3C82), spotColor = Color(0x2E0A3C82))
            .then(if (clipContent) Modifier.clip(shape) else Modifier)
            .background(Color.White, shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/** Big rounded button with a darker bottom edge, as in the prototype. */
@Composable
fun ChunkyButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    height: Dp = 60.dp,
    fontSize: Int = 21,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val fill = if (enabled) color else Color(0xFFE3EAF4)
    val ink = if (enabled) textColor else Color(0xFF2F3F66)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .clip(RoundedCornerShape(20.dp))
            .background(fill)
            .drawBehind {
                drawRect(Color(0x2E000000), topLeft = Offset(0f, size.height - 5.dp.toPx()))
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp, if (leading == null && trailing == null) Alignment.CenterHorizontally else Alignment.Start),
    ) {
        leading?.invoke(this)
        Text(
            text,
            color = ink,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            modifier = if (leading != null || trailing != null) Modifier.weight(1f) else Modifier,
            textAlign = if (leading == null && trailing == null) TextAlign.Center else TextAlign.Start,
        )
        trailing?.invoke(this)
    }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(2.dp, Color(0xFFCFDCEE), RoundedCornerShape(999.dp))
            .background(Color.White)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = MQ.Ink) }
}

@Composable
fun RoundIconButton(onClick: () -> Unit, contentDescription: String, light: Boolean = false, content: @Composable () -> Unit = {
    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = contentDescription, tint = if (light) Color.White else MQ.Ink)
}) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (light) Color(0x38FFFFFF) else MQ.TintBlue)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Back button, centred title, and an optional slot on the right. */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?, trailing: @Composable () -> Unit = { Spacer(Modifier.width(44.dp)) }) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) RoundIconButton(onClick = onBack, contentDescription = "Back") else Spacer(Modifier.width(44.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Gold Coin drawn in code so it scales cleanly. */
@Composable
fun CoinIcon(size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = Offset(r, r)
        drawCircle(MQ.Gold, r * 0.95f, c)
        drawCircle(MQ.GoldDark, r * 0.95f, c, style = Stroke(r * 0.12f))
        val star = Path()
        for (k in 0 until 10) {
            val a = -Math.PI / 2 + k * Math.PI / 5
            val rad = if (k % 2 == 0) r * 0.55f else r * 0.24f
            val p = Offset(c.x + rad * cos(a).toFloat(), c.y + rad * sin(a).toFloat())
            if (k == 0) star.moveTo(p.x, p.y) else star.lineTo(p.x, p.y)
        }
        star.close()
        drawPath(star, Color(0xFFFFE27A))
        drawPath(star, MQ.GoldDark, style = Stroke(r * 0.07f))
    }
}

@Composable
fun CoinPill(amount: String, modifier: Modifier = Modifier, big: Boolean = false) {
    Row(
        modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MQ.GoldTint)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoinIcon(if (big) 28.dp else 20.dp)
        Text(amount, color = MQ.GoldText, fontWeight = FontWeight.ExtraBold, fontSize = if (big) 24.sp else 16.sp)
    }
}

@Composable
fun Tag(text: String, background: Color, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    )
}

/** Leo with a speech bubble pointing at him. */
@Composable
fun LeoSays(face: LeoFace, text: String, modifier: Modifier = Modifier, label: String? = null, size: Dp = 80.dp, bob: Boolean = true) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Leo(face, size, bob = bob)
        Column(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(MQ.Tint)
                .border(2.dp, MQ.Line, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (label != null) Text(label, color = MQ.DeepOrange, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(text, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 10.dp, track: Color = MQ.Line) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
fun Avatar(initial: String, colour: Long, size: Dp = 58.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(colour))
            .border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Text(initial, fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Bold, color = MQ.Ink) }
}
