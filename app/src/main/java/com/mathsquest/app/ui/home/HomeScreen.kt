package com.mathsquest.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mathsquest.app.ui.components.Avatar
import com.mathsquest.app.ui.components.ChunkyButton
import com.mathsquest.app.ui.components.CoinIcon
import com.mathsquest.app.ui.components.LeoFace
import com.mathsquest.app.ui.components.LeoSays
import com.mathsquest.app.ui.components.ProgressBar
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.Tag
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.theme.MQ
import com.mathsquest.core.CoinRules
import com.mathsquest.core.DailyChallenge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeState(
    val childId: Long = 0,
    val name: String = "",
    val grade: Int = 0,
    val colour: Long = 0xFFFFE08A,
    val coins: Int = 0,
    val level: Int = 1,
    val levelProgress: Float = 0f,
    val streak: Int = 0,
    val pending: Int = 0,
    val dailyDone: Boolean = false,
    val cheer: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(handle: SavedStateHandle, repo: MathsQuestRepository) : ViewModel() {
    private val childId: Long = checkNotNull(handle[Args.CHILD_ID])

    val state: StateFlow<HomeState> = combine(
        repo.observeChild(childId).filterNotNull(),
        repo.observeAvailable(childId),
        repo.observeRequestsFor(childId),
        repo.observeRewards(),
    ) { child, coins, requests, rewards ->
        val dailyDone = child.dailyDoneDay == repo.today()
        val next = rewards.firstOrNull { it.cost > coins }
        HomeState(
            childId = child.id,
            name = child.name,
            grade = child.grade,
            colour = child.avatarColor,
            coins = coins,
            level = CoinRules.level(child.xp),
            levelProgress = CoinRules.levelProgress(child.xp),
            streak = repo.displayedStreak(child),
            pending = requests.count { it.status == "PENDING" },
            dailyDone = dailyDone,
            cheer = when {
                !dailyDone -> "Ready for today's Daily Challenge? You can do it!"
                next != null -> "Only ${next.cost - coins} coins to your next reward!"
                else -> "Wow, you can pick any reward!"
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}

@Composable
fun HomeScreen(
    onPlay: (Long) -> Unit,
    onDaily: (Long) -> Unit,
    onProgress: (Long) -> Unit,
    onRewards: (Long) -> Unit,
    onSwitch: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    SkyScreen {
        WhiteCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Avatar(s.name.take(1).uppercase(), s.colour, 64.dp)
                Column(Modifier.weight(1f)) {
                    Text(s.name, style = MaterialTheme.typography.headlineMedium)
                    Text("Grade ${s.grade}", style = MaterialTheme.typography.bodyMedium, color = MQ.Muted)
                }
                Text(
                    "Switch",
                    color = MQ.Blue,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onSwitch)
                        .padding(12.dp),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF2F8FF))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MQ.Gold, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Level ${s.level}", style = MaterialTheme.typography.titleLarge)
                    ProgressBar(s.levelProgress, MQ.Gold, height = 12.dp)
                }
                Text("${Math.round(s.levelProgress * 100)}%", style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Modifier.weight(1f), "${s.coins}", "Coins") { CoinIcon(32.dp) }
                StatTile(Modifier.weight(1f), "${s.streak}", "Day streak") {
                    Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = MQ.Orange, modifier = Modifier.size(30.dp))
                }
            }
        }

        LeoSays(LeoFace.Happy, s.cheer, label = "Leo says", size = 104.dp)

        ChunkyButton(
            text = "Daily Challenge",
            color = Color(0xFFFFF4D1),
            textColor = MQ.Ink,
            onClick = { onDaily(s.childId) },
            height = 72.dp,
            leading = { Icon(Icons.Filled.DateRange, contentDescription = null, tint = MQ.Pink, modifier = Modifier.size(36.dp)) },
            trailing = {
                if (s.dailyDone) Tag("Done", MQ.GreenTint, MQ.GreenText) else Tag("${DailyChallenge.maxCoins} coins", MQ.Pink, Color.White)
            },
        )

        WhiteCard {
            ChunkyButton("Play Maths", MQ.Green, onClick = { onPlay(s.childId) }, leading = {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            })
            ChunkyButton("My Progress", MQ.Purple, onClick = { onProgress(s.childId) }, leading = {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            })
            ChunkyButton(
                "Rewards", MQ.Orange, onClick = { onRewards(s.childId) },
                leading = { Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) },
                trailing = { if (s.pending > 0) Tag("${s.pending} waiting", Color.White, Color(0xFF9A3A00)) },
            )
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, value: String, label: String, icon: @Composable () -> Unit) {
    Row(
        modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(2.dp, MQ.Line, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        icon()
        Column {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MQ.Ink)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
