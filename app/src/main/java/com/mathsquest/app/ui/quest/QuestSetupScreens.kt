package com.mathsquest.app.ui.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mathsquest.app.data.repo.MathsQuestRepository
import com.mathsquest.app.ui.Args
import com.mathsquest.app.ui.components.CoinIcon
import com.mathsquest.app.ui.components.LeoFace
import com.mathsquest.app.ui.components.LeoSays
import com.mathsquest.app.ui.components.ScreenHeader
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.Tag
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.theme.MQ
import com.mathsquest.core.CoinRules
import com.mathsquest.core.Difficulty
import com.mathsquest.core.Grades
import com.mathsquest.core.Topic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Shared by the grade, topic and difficulty screens. */
@HiltViewModel
class QuestSetupViewModel @Inject constructor(handle: SavedStateHandle, repo: MathsQuestRepository) : ViewModel() {
    val childId: Long = checkNotNull(handle[Args.CHILD_ID])
    val grade: Int = handle[Args.GRADE] ?: 0
    val topic: Topic = handle.get<String>(Args.TOPIC)?.let { Topic.valueOf(it) } ?: Topic.ADD
    val childGrade: StateFlow<Int> = repo.observeChild(childId).filterNotNull().map { it.grade }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

private val GRADE_COLOURS = mapOf(
    3 to MQ.Green, 4 to MQ.Blue, 5 to MQ.Purple, 6 to MQ.DeepOrange, 7 to MQ.Teal, 8 to MQ.Pink, 9 to MQ.Indigo,
)

@Composable
fun GradeScreen(onBack: () -> Unit, onPick: (Long, Int) -> Unit, vm: QuestSetupViewModel = hiltViewModel()) {
    val childGrade by vm.childGrade.collectAsStateWithLifecycle()
    SkyScreen {
        WhiteCard(spacing = 16.dp) {
            ScreenHeader("Select Grade", onBack)
            LeoSays(LeoFace.Waving, "Hi! Choose your grade so I can pick the right questions.", size = 76.dp)
            Grades.all.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { g ->
                        val tag = when {
                            g == childGrade -> "Your grade"
                            childGrade > 0 && CoinRules.isPracticeGrade(childGrade, g) -> "Practice"
                            else -> null
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 92.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(GRADE_COLOURS.getValue(g))
                                .bottomEdge()
                                .clickable(role = Role.Button, onClickLabel = "Grade $g") { onPick(vm.childId, g) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        ) {
                            Text("$g", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                            Text("Grade $g", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            if (tag != null) Tag(tag, Color(0xEBFFFFFF), MQ.Ink)
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

private val TOPIC_COLOURS = mapOf(
    Topic.ADD to MQ.Green, Topic.SUB to MQ.DeepOrange, Topic.MUL to MQ.Purple, Topic.DIV to MQ.Blue, Topic.MIXED to MQ.Pink,
)

@Composable
fun TopicScreen(onBack: () -> Unit, onPick: (Long, Int, String) -> Unit, vm: QuestSetupViewModel = hiltViewModel()) {
    SkyScreen {
        WhiteCard(spacing = 14.dp) {
            ScreenHeader("Choose a Topic", onBack)
            LeoSays(LeoFace.Pointing, "Grade ${vm.grade}! What do you want to practise today?", size = 76.dp)
            Topic.entries.forEach { t ->
                val colour = TOPIC_COLOURS.getValue(t)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 70.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colour)
                        .bottomEdge()
                        .clickable(role = Role.Button) { onPick(vm.childId, vm.grade, t.name) }
                        .padding(start = 12.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Text(t.operation?.symbol ?: "★", color = colour, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(t.label, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun DifficultyScreen(onBack: () -> Unit, onPick: (Long, Int, String, Int) -> Unit, vm: QuestSetupViewModel = hiltViewModel()) {
    val childGrade by vm.childGrade.collectAsStateWithLifecycle()
    val practice = childGrade > 0 && CoinRules.isPracticeGrade(childGrade, vm.grade)
    SkyScreen {
        WhiteCard(spacing = 14.dp) {
            ScreenHeader("Choose Your Challenge", onBack)
            LeoSays(LeoFace.Ready, "${vm.topic.label}, Grade ${vm.grade}. More challenge means more coins. Ready?", size = 76.dp)
            if (practice) {
                Text(
                    "Grade ${vm.grade} is a practice level for you. Great for warming up, but coins start at Grade ${childGrade - 1}.",
                    color = Color(0xFF173A7A),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MQ.TintBlue).padding(12.dp),
                )
            }
            Difficulty.entries.forEach { d ->
                val (bg, ink) = when (d) {
                    Difficulty.EASY -> MQ.Green to Color.White
                    Difficulty.MODERATE -> MQ.Gold to Color(0xFF3D2600)
                    Difficulty.TOUGH -> MQ.Red to Color.White
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 92.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(bg)
                        .bottomEdge()
                        .clickable(role = Role.Button) { onPick(vm.childId, vm.grade, vm.topic.name, d.level) }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(d.label.uppercase(), color = ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CoinIcon(22.dp)
                            Text(
                                if (practice) "Practice · 0 coins" else "${d.baseCoins} ${if (d.baseCoins == 1) "Coin" else "Coins"}",
                                color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Text("5 questions · each right answer earns coins", style = MaterialTheme.typography.labelMedium, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** The darker strip along the bottom of the chunky buttons. */
fun Modifier.bottomEdge(): Modifier = drawBehind {
    drawRect(Color(0x33000000), topLeft = Offset(0f, size.height - 5.dp.toPx()))
}
