package com.mathsquest.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathsquest.app.ui.theme.MQ
import com.mathsquest.core.Tally

/**
 * Draws tally marks in fives (four strokes and a slash). Addition shows two bundles joined by "+";
 * subtraction crosses out marks in red; multiplication and division circle equal groups.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TallyMarks(tally: Tally, modifier: Modifier = Modifier) {
    val circled = tally.joiner.isEmpty() && tally.groups.size > 1
    val description = when {
        tally.crossedOut > 0 -> "${tally.groups.first()} tally marks with ${tally.crossedOut} crossed out"
        tally.joiner == "+" -> tally.groups.joinToString(" plus ") { "$it tally marks" }
        else -> "${tally.groups.size} groups of ${tally.groups.first()} tally marks"
    }
    FlowRow(
        modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        tally.groups.forEachIndexed { index, count ->
            if (index > 0 && tally.joiner.isNotEmpty()) {
                Text(tally.joiner, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MQ.Ink, modifier = Modifier.align(Alignment.CenterVertically))
            }
            val crossed = if (index == 0) tally.crossedOut else 0
            Row(
                (if (circled) Modifier.border(2.dp, MQ.Blue, RoundedCornerShape(14.dp)).padding(horizontal = 8.dp, vertical = 6.dp) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var drawn = 0
                while (drawn < count) {
                    val inThisFive = minOf(5, count - drawn)
                    val firstCrossed = count - crossed // marks at index >= this are crossed out
                    FiveMarks(inThisFive, crossedFrom = (firstCrossed - drawn).coerceIn(0, 5))
                    drawn += inThisFive
                }
            }
        }
    }
}

/** Up to five tally strokes; strokes from index [crossedFrom] on are red and struck through. */
@Composable
private fun FiveMarks(count: Int, crossedFrom: Int) {
    Canvas(Modifier.size(width = 34.dp, height = 30.dp)) {
        val stroke = 3.dp.toPx()
        val gap = size.width / 5f
        val top = 2.dp.toPx()
        val bottom = size.height - 2.dp.toPx()
        for (i in 0 until minOf(count, 4)) {
            val x = gap * (i + 0.7f)
            val crossed = i >= crossedFrom
            drawLine(if (crossed) MQ.Red.copy(alpha = 0.55f) else MQ.Ink, Offset(x, top), Offset(x, bottom), stroke, StrokeCap.Round)
            if (crossed) drawLine(MQ.Red, Offset(x - gap * 0.45f, top + 6f), Offset(x + gap * 0.45f, bottom - 6f), stroke * 0.8f, StrokeCap.Round)
        }
        if (count == 5) {
            val crossed = 4 >= crossedFrom
            drawLine(if (crossed) MQ.Red else MQ.Ink, Offset(0f, bottom - 4f), Offset(size.width, top + 4f), stroke, StrokeCap.Round)
        }
    }
}
