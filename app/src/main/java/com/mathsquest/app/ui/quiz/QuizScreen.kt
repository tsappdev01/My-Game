package com.mathsquest.app.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mathsquest.app.ui.components.ChunkyButton
import com.mathsquest.app.ui.components.CoinIcon
import com.mathsquest.app.ui.components.CoinPill
import com.mathsquest.app.ui.components.ConfettiBurst
import com.mathsquest.app.ui.components.FlyIn
import com.mathsquest.app.ui.components.TallyMarks
import com.mathsquest.core.Tally
import com.mathsquest.app.ui.components.Leo
import com.mathsquest.app.ui.components.LeoFace
import com.mathsquest.app.ui.components.LeoSays
import com.mathsquest.app.ui.components.OutlineButton
import com.mathsquest.app.ui.components.ProgressBar
import com.mathsquest.app.ui.components.RoundIconButton
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.quest.bottomEdge
import com.mathsquest.app.ui.theme.MQ
import com.mathsquest.core.Operation
import com.mathsquest.core.QuestionEngine

@Composable
fun QuizScreen(
    onExit: (Long) -> Unit,
    onPlayAgain: (Long, Int, String, Int) -> Unit,
    vm: QuizViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    BackHandler { onExit(s.childId) }
    LaunchedEffect(s.celebrate) { if (s.celebrate > 0) haptics.performHapticFeedback(HapticFeedbackType.LongPress) }
    if (s.phase == Phase.DONE) {
        Box(Modifier.fillMaxSize()) {
            Summary(s, onHome = { onExit(s.childId) }, onAgain = { onPlayAgain(s.childId, s.grade, s.topic.name, s.difficulty.level) })
            ConfettiBurst(s.celebrate, pieces = 110)
        }
        return
    }
    val q = s.question
    Box(Modifier.fillMaxSize()) {
    SkyScreen {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundIconButton(onClick = { onExit(s.childId) }, contentDescription = "End quest", light = true) {
                Icon(Icons.Filled.Close, contentDescription = "End quest", tint = Color.White)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${if (s.daily) "Daily Challenge" else "Question"} ${s.index + 1} / ${s.total}",
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                ProgressBar(s.answered / s.total.toFloat(), Color(0xFF3DDC6E), track = Color(0x59FFFFFF))
            }
            CoinPill("${s.worthCoins}")
        }

        WhiteCard(spacing = 8.dp, clipContent = false) {
            Text(
                s.worthText,
                color = Color(0xFF173A7A), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(999.dp)).background(MQ.TintBlue)
                    .padding(horizontal = 12.dp, vertical = 3.dp),
            )
            if (q != null) Problem(s.index, q.operation, q.a, q.b, answerText(s), answerColour(s), onLand = vm::playDrop)
        }

        when (s.phase) {
            Phase.ASK, Phase.LOADING -> {
                LeoSays(LeoFace.Focused, if (s.attempt > 1) "Use the hint. You can do it!" else "Take your time!", size = 70.dp)
                Keypad(onDigit = vm::type, onDelete = vm::delete, onCheck = vm::check, enabled = s.phase == Phase.ASK)
            }
            Phase.RIGHT -> RightPanel(s, onNext = vm::next, onCoinLand = { if (s.earned > 0) vm.playDrop() })
            Phase.WRONG -> WrongPanel(s, onRetry = vm::retry)
            Phase.REVEAL -> RevealPanel(s, onNext = vm::next)
            Phase.DONE -> Unit
        }
    }
    if (s.phase == Phase.RIGHT) ConfettiBurst(s.celebrate)
    s.levelUp?.let { level -> LevelUpParty(level, s.celebrate, onClose = vm::dismissLevelUp) }
    }
}

/** Full-screen party when the child reaches a new level. */
@Composable
private fun LevelUpParty(level: Int, trigger: Int, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC0A2A5E))
            .clickable(interactionSource = null, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
            WhiteCard(modifier = Modifier.padding(24.dp), spacing = 12.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Leo(LeoFace.Proud, 150.dp, dance = trigger)
                    Text("Level up!", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MQ.Purple)
                    Text("You reached Level $level", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Text("Keep solving to fill the next star bar.", style = MaterialTheme.typography.bodyLarge, color = MQ.Muted, textAlign = TextAlign.Center)
                }
                ChunkyButton("Keep going!", MQ.Purple, onClick = onClose)
            }
            ConfettiBurst(trigger, pieces = 140)
    }
}

private fun answerText(s: QuizState): String = when {
    s.phase == Phase.RIGHT || s.phase == Phase.REVEAL -> QuestionEngine.format(s.question!!.answer)
    s.typed.isNotEmpty() -> QuestionEngine.format(s.typed.toLong())
    else -> "?"
}

private fun answerColour(s: QuizState): Color = when {
    s.phase == Phase.RIGHT -> MQ.Green
    s.typed.isNotEmpty() || s.phase == Phase.REVEAL -> MQ.Blue
    else -> Color(0xFF8796B8)
}

/**
 * Column layout for +, − and ×; one line for ÷. Each part flies in from a different side and lands
 * with a coin clink: first number from the top-left corner, the sign from the left, the second number
 * from the right, and the answer from the bottom. Replays for each new question ([key]).
 */
@Composable
private fun Problem(key: Any, op: Operation, a: Long, b: Long, answer: String, answerColour: Color, onLand: () -> Unit) {
    val fa = QuestionEngine.format(a)
    val fb = QuestionEngine.format(b)
    val longest = maxOf(fa.length, fb.length, answer.length)
    val size = when {
        op == Operation.DIV -> if (longest > 4) 34.sp else 44.sp
        longest > 6 -> 38.sp
        else -> 50.sp
    }
    @Composable
    fun Part(text: String, colour: Color = MQ.Ink) = Text(text, fontSize = size, fontWeight = FontWeight.Bold, color = colour)

    Box(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, contentAlignment = Alignment.Center) {
        if (op == Operation.DIV) {
            Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                FlyIn(key, -1f, -1f, 0, onLand = onLand) { Part(fa) }
                FlyIn(key, -1f, 0f, 220, onLand = onLand) { Part("÷") }
                FlyIn(key, 1f, -1f, 440, onLand = onLand) { Part(fb) }
                FlyIn(key, 1f, 0f, 660) { Part("=") }
                FlyIn(key, 0f, 1f, 820, onLand = onLand) { Part(answer, answerColour) }
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                FlyIn(key, -1f, -1f, 0, onLand = onLand) { Part(fa) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlyIn(key, -1f, 0f, 220, Modifier.padding(end = 16.dp), onLand = onLand) { Part(op.symbol) }
                    FlyIn(key, 1f, 0f, 440, onLand = onLand) { Part(fb) }
                }
                FlyIn(key, 1f, 0f, 660) {
                    Box(Modifier.width((longest * size.value * 0.62f + 50).dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MQ.Ink))
                }
                FlyIn(key, 0f, 1f, 820, onLand = onLand) { Part(answer, answerColour) }
            }
        }
    }
}

@Composable
private fun Keypad(onDigit: (Char) -> Unit, onDelete: () -> Unit, onCheck: () -> Unit, enabled: Boolean) {
    val rows = listOf("123", "456", "789")
    Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                r.forEach { d -> Key(Modifier.weight(1f).testTag("key-$d"), MQ.Blue, "Digit $d", enabled, { onDigit(d) }) { KeyText("$d") } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Key(Modifier.weight(1f).testTag("key-delete"), MQ.Red, "Delete digit", enabled, onDelete) { KeyText("⌫") }
            Key(Modifier.weight(1f).testTag("key-0"), MQ.Blue, "Digit 0", enabled, { onDigit('0') }) { KeyText("0") }
            Key(Modifier.weight(1f).testTag("key-check"), MQ.Green, "Check answer", enabled, onCheck) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.height(32.dp).width(32.dp))
            }
        }
    }
}

@Composable
private fun Key(modifier: Modifier, colour: Color, label: String, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colour)
            .bottomEdge()
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun KeyText(text: String) = Text(text, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)

@Composable
private fun RightPanel(s: QuizState, onNext: () -> Unit, onCoinLand: () -> Unit) {
    WhiteCard(padding = 0.dp, spacing = 10.dp, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(
            "Correct!", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().background(MQ.Green).padding(14.dp),
        )
        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Leo(LeoFace.Cheering, 88.dp, dance = s.celebrate)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FlyIn(s.celebrate, 1f, 1f, 150, onLand = onCoinLand) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CoinIcon(30.dp)
                            Text("+${s.earned} ${if (s.earned == 1) "Coin" else "Coins"}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MQ.Ink)
                        }
                    }
                    FlyIn(s.celebrate, 1f, 0f, 380) {
                        Text("+${s.xp} XP", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MQ.Purple)
                    }
                }
            }
            s.note?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF2F3F66), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
            ChunkyButton(if (s.index + 1 >= s.total) "See results" else "Next Question", MQ.Green, onClick = onNext)
        }
    }
}

@Composable
private fun WrongPanel(s: QuizState, onRetry: () -> Unit) {
    WhiteCard(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Leo(LeoFace.Thinking, 84.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Almost!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC42B2B))
                Text("Let's work it out together.", style = MaterialTheme.typography.titleMedium)
                Text("Leo has a clue for you", style = MaterialTheme.typography.labelMedium, color = MQ.DeepOrange)
            }
        }
        s.question?.let { q -> Clue(q.technique, q.tally, q.hint) }
        ChunkyButton("Try Again", MQ.Orange, onClick = onRetry)
    }
}

@Composable
private fun RevealPanel(s: QuizState, onNext: () -> Unit) {
    WhiteCard(modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text("The answer is ${QuestionEngine.format(s.question!!.answer)}", style = MaterialTheme.typography.headlineSmall)
        Text("Here's how it works:", style = MaterialTheme.typography.labelMedium)
        Clue(s.question.technique, s.question.tally, s.question.steps)
        ChunkyButton(if (s.index + 1 >= s.total) "See results" else "Next Question", MQ.Blue, onClick = onNext)
    }
}

/** The method's name ("Leo's trick"), a tally picture for small numbers, then the steps. */
@Composable
private fun Clue(technique: String, tally: Tally?, lines: List<String>) {
    if (technique.isNotBlank()) {
        Text(
            "Leo's trick: $technique",
            color = MQ.Purple,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFFEDE6FF)).padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
    if (tally != null) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFFBEA)).padding(12.dp)) {
            TallyMarks(tally)
        }
    }
    Steps(lines)
}

@Composable
private fun Steps(lines: List<String>) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MQ.Tint).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { lines.forEach { Text(it, fontSize = 18.sp, color = MQ.Ink) } }
}

@Composable
private fun Summary(s: QuizState, onHome: () -> Unit, onAgain: () -> Unit) {
    SkyScreen {
        WhiteCard(spacing = 14.dp) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Leo(LeoFace.Proud, 120.dp, dance = s.celebrate)
                Text(if (s.daily) "Challenge complete!" else "Quest complete!", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                Text("${s.firstTryRight} of ${s.total} right first time", style = MaterialTheme.typography.titleLarge, color = Color(0xFF2F3F66))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CoinPill("+${s.roundCoins}", big = true)
                    Text(
                        "+${s.roundXp} XP", color = Color(0xFF4A23A8), fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(Color(0xFFEDE6FF)).padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (s.perfect) {
                    Text(
                        "Perfect round! Badge unlocked.", color = MQ.GreenText, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(MQ.GreenTint).padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            s.summaryNotes.forEach { note ->
                Text(
                    note, color = Color(0xFF173A7A), style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MQ.TintBlue).padding(12.dp),
                )
            }
            if (!s.daily) ChunkyButton("Play again", MQ.Green, onClick = onAgain)
            OutlineButton("Back home", onClick = onHome, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp))
        }
    }
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}
