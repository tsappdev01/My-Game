package com.mathsquest.app.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mathsquest.app.data.repo.MathsQuestRepository
import com.mathsquest.app.data.repo.WeekSummary
import com.mathsquest.app.ui.Args
import com.mathsquest.app.ui.components.LeoFace
import com.mathsquest.app.ui.components.LeoSays
import com.mathsquest.app.ui.components.ProgressBar
import com.mathsquest.app.ui.components.ScreenHeader
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.theme.MQ
import com.mathsquest.core.Badge
import com.mathsquest.core.Badges
import com.mathsquest.core.CoinRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProgressState(
    val week: WeekSummary = WeekSummary(0, 0, 0, 0),
    val streak: Int = 0,
    val level: Int = 1,
    val levelProgress: Float = 0f,
    val badges: List<Badge> = emptyList(),
    val coach: String = "",
)

@HiltViewModel
class ProgressViewModel @Inject constructor(handle: SavedStateHandle, repo: MathsQuestRepository) : ViewModel() {
    private val childId: Long = checkNotNull(handle[Args.CHILD_ID])
    val state: StateFlow<ProgressState> = combine(
        repo.observeChild(childId).filterNotNull(),
        repo.observeWeek(childId),
        repo.observeOperationStats(childId),
    ) { child, week, ops ->
        val streak = repo.displayedStreak(child)
        val badges = Badges.evaluate(ops, streak, child.hadPerfectRound)
        val next = badges.firstOrNull { it.id == "streak7" && !it.earned } ?: badges.firstOrNull { !it.earned }
        ProgressState(
            week = week,
            streak = streak,
            level = CoinRules.level(child.xp),
            levelProgress = CoinRules.levelProgress(child.xp),
            badges = badges,
            coach = if (next == null) "You have earned every badge. Amazing!"
            else "So close! ${next.status.replaceFirstChar { it.uppercase() }} to earn ${next.name}.",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())
}

private val BADGE_COLOURS = mapOf(
    "add_master" to MQ.Green, "mul_master" to MQ.Purple, "div_champion" to MQ.Blue,
    "streak7" to MQ.Orange, "q100" to Color(0xFFB07800), "perfect" to MQ.Pink,
)

@Composable
fun ProgressScreen(onBack: () -> Unit, vm: ProgressViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    SkyScreen {
        WhiteCard(spacing = 16.dp) {
            ScreenHeader("My Progress", onBack)
            LeoSays(LeoFace.Coach, s.coach, label = "Coach Leo", size = 88.dp)
            val tiles = listOf(
                "Questions this week" to "${s.week.answered}",
                "Right first time" to "${s.week.accuracyPercent}%",
                "Coins this week" to "${s.week.coinsEarned}",
                "Day streak" to "${s.streak}",
            )
            tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { (label, value) ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(MQ.Tint)
                                .border(2.dp, MQ.Line, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MQ.Ink)
                        }
                    }
                }
            }
            Text("Badges", style = MaterialTheme.typography.headlineSmall)
            s.badges.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { b ->
                        Column(
                            Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "${b.name}: ${b.status}" },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier.size(64.dp).clip(CircleShape)
                                    .background(if (b.earned) BADGE_COLOURS[b.id] ?: MQ.Blue else Color(0xFFB3C0D6))
                                    .border(4.dp, if (b.earned) Color(0xFFFFD43B) else Color(0xFFE3EAF4), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text(b.glyph, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                            Text(b.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = MQ.Ink)
                            Text(b.status, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MQ.Tint).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Level ${s.level}", style = MaterialTheme.typography.titleLarge)
                    ProgressBar(s.levelProgress, MQ.Gold, height = 12.dp)
                }
                Text("${Math.round(s.levelProgress * 100)}%", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
