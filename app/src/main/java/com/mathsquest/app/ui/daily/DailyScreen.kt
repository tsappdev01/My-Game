package com.mathsquest.app.ui.daily

import androidx.compose.foundation.background
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
import com.mathsquest.app.ui.Args
import com.mathsquest.app.ui.components.ChunkyButton
import com.mathsquest.app.ui.components.CoinIcon
import com.mathsquest.app.ui.components.Leo
import com.mathsquest.app.ui.components.LeoFace
import com.mathsquest.app.ui.components.RoundIconButton
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.theme.MQ
import com.mathsquest.core.CoinRules
import com.mathsquest.core.DailyChallenge
import com.mathsquest.core.Difficulty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DailyState(val grade: Int = 0, val done: Boolean = false, val room: Int = Int.MAX_VALUE, val streakNext: Boolean = false)

@HiltViewModel
class DailyViewModel @Inject constructor(handle: SavedStateHandle, repo: MathsQuestRepository) : ViewModel() {
    val childId: Long = checkNotNull(handle[Args.CHILD_ID])
    val state: StateFlow<DailyState> = combine(repo.observeChild(childId).filterNotNull(), repo.observeEarnedThisMonth(childId)) { c, earned ->
        val today = repo.today()
        DailyState(
            grade = c.grade,
            done = c.dailyDoneDay == today,
            room = (c.monthlyCapCoins - earned).coerceAtLeast(0),
            streakNext = c.lastPracticeDay != today && repo.displayedStreak(c) + 1 == CoinRules.STREAK_BONUS_DAYS,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyState())
}

@Composable
fun DailyScreen(onBack: () -> Unit, onStart: (Long, Int) -> Unit, vm: DailyViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    SkyScreen {
        WhiteCard(spacing = 14.dp) {
            RoundIconButton(onClick = onBack, contentDescription = "Back")
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Leo(if (s.done) LeoFace.Sleepy else LeoFace.Excited, 104.dp)
                Text("Daily Challenge", style = MaterialTheme.typography.displaySmall)
                Text("Solve 10 questions today and earn bonus coins!", style = MaterialTheme.typography.bodyLarge, color = MQ.Muted, textAlign = TextAlign.Center)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MQ.Tint).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Difficulty.entries.forEach { d ->
                    val count = DailyChallenge.difficulties.count { it == d }
                    val (bg, ink) = when (d) {
                        Difficulty.EASY -> MQ.Green to Color.White
                        Difficulty.MODERATE -> MQ.Gold to Color(0xFF3D2600)
                        Difficulty.TOUGH -> MQ.Red to Color.White
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
                            Text("$count", color = ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Text(d.label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MQ.Ink, modifier = Modifier.weight(1f))
                        Text("${d.baseCoins} ${if (d.baseCoins == 1) "coin" else "coins"} each", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFFFF4D1)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Potential Reward", fontWeight = FontWeight.ExtraBold, color = Color(0xFF3D2600))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CoinIcon(40.dp)
                    Text("${DailyChallenge.maxCoins} Coins", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3D2600))
                }
                if (s.streakNext && !s.done) {
                    Text("Finish today for a 7-day streak and +${CoinRules.STREAK_BONUS_COINS} bonus coins!", fontWeight = FontWeight.ExtraBold, color = Color(0xFF9A3A00), textAlign = TextAlign.Center)
                }
            }
            if (!s.done && s.room < DailyChallenge.maxCoins) {
                Text(
                    if (s.room == 0) "Your coin jar is full this month, so today you play for XP and badges."
                    else "Your coin jar only has room for ${s.room} more coins this month.",
                    color = Color(0xFF173A7A), style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MQ.TintBlue).padding(12.dp),
                )
            }
            if (s.done) {
                Text(
                    "Leo is resting. Come back tomorrow!", color = MQ.GreenText, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MQ.GreenTint).padding(18.dp),
                )
            } else {
                ChunkyButton("Start Challenge", MQ.Blue, enabled = s.grade > 0, onClick = { onStart(vm.childId, s.grade) })
            }
            Text("One challenge a day · mixed topics at your grade", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
