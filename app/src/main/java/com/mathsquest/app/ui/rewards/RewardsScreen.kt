package com.mathsquest.app.ui.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mathsquest.app.data.local.RequestStatus
import com.mathsquest.app.data.local.RewardEntity
import com.mathsquest.app.data.local.RewardRequestEntity
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RewardsState(
    val available: Int = 0,
    val rewards: List<RewardEntity> = emptyList(),
    val requests: List<RewardRequestEntity> = emptyList(),
) {
    val pendingRewardIds: Set<Long> get() = requests.filter { it.status == RequestStatus.PENDING }.map { it.rewardId }.toSet()
    val leoLine: String get() {
        val next = rewards.firstOrNull { it.cost > available }
        return when {
            pendingRewardIds.isNotEmpty() -> "Fingers crossed! Your parent will check your request soon."
            rewards.any { it.cost <= available } -> "You can redeem a reward! Which one will you pick?"
            next != null -> "Keep going! ${next.cost - available} more coins for ${next.title.lowercase()}."
            else -> "Keep practising to earn Gold Coins!"
        }
    }
}

@HiltViewModel
class RewardsViewModel @Inject constructor(handle: SavedStateHandle, private val repo: MathsQuestRepository) : ViewModel() {
    private val childId: Long = checkNotNull(handle[Args.CHILD_ID])
    val state: StateFlow<RewardsState> = combine(
        repo.observeAvailable(childId), repo.observeRewards(), repo.observeRequestsFor(childId),
    ) { a, r, q -> RewardsState(a, r, q) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RewardsState())

    fun redeem(rewardId: Long) {
        viewModelScope.launch { repo.requestReward(childId, rewardId) }
    }
}

private fun iconFor(key: String): Pair<ImageVector, Color> = when (key) {
    "screen" -> Icons.Filled.PlayArrow to MQ.Blue
    "dinner" -> Icons.Filled.Home to MQ.Orange
    "icecream" -> Icons.Filled.Favorite to MQ.Pink
    "park" -> Icons.Filled.Place to MQ.Green
    "toy" -> Icons.Filled.Face to MQ.Purple
    else -> Icons.Filled.Star to MQ.Indigo
}

@Composable
fun RewardsScreen(onBack: () -> Unit, vm: RewardsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    SkyScreen {
        WhiteCard(spacing = 14.dp) {
            ScreenHeader("Reward Shop", onBack)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MQ.Gold).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CoinIcon(46.dp)
                Column(Modifier.weight(1f)) {
                    Text("Your Coins", fontWeight = FontWeight.ExtraBold, color = Color(0xFF3D2600))
                    Text("${s.available}", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3D2600))
                }
            }
            LeoSays(LeoFace.Hopeful, s.leoLine, size = 80.dp)
            Text("Choose your reward", style = MaterialTheme.typography.headlineSmall)
            s.rewards.forEach { r ->
                val asked = r.id in s.pendingRewardIds
                val can = !asked && s.available >= r.cost
                val (icon, tile) = iconFor(r.icon)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).border(2.dp, MQ.Line, RoundedCornerShape(18.dp)).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(tile), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleMedium)
                        Text("Unlock with ${r.cost} coins", style = MaterialTheme.typography.labelMedium)
                    }
                    Text(
                        when { asked -> "Asked"; can -> "Redeem"; else -> "${r.cost - s.available} more" },
                        color = if (can) Color.White else Color(0xFF2F3F66),
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(min = 84.dp)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (can) MQ.Blue else Color(0xFFE3EAF4))
                            .clickable(enabled = can, role = Role.Button) { vm.redeem(r.id) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                    )
                }
            }
            if (s.requests.isNotEmpty()) {
                Text("My requests", style = MaterialTheme.typography.headlineSmall)
                s.requests.forEach { q ->
                    val (label, bg, fg) = when (q.status) {
                        RequestStatus.APPROVED -> Triple("Approved", MQ.GreenTint, MQ.GreenText)
                        RequestStatus.DECLINED -> Triple("Not this time", MQ.RedTint, MQ.RedText)
                        else -> Triple("Waiting", MQ.GoldTint, MQ.GoldText)
                    }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MQ.Tint).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(q.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Tag(label, bg, fg)
                    }
                }
            }
            Text("A parent approves every reward.", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
