package com.mathsquest.app.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mathsquest.app.data.repo.MathsQuestRepository
import com.mathsquest.app.ui.components.Avatar
import com.mathsquest.app.ui.components.CoinPill
import com.mathsquest.app.ui.components.Leo
import com.mathsquest.app.ui.components.LeoFace
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.theme.MQ
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileCard(val id: Long, val name: String, val grade: Int, val colour: Long, val coins: Int)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfilesViewModel @Inject constructor(repo: MathsQuestRepository) : ViewModel() {
    val profiles: StateFlow<List<ProfileCard>> = repo.observeChildren().flatMapLatest { kids ->
        if (kids.isEmpty()) flowOf(emptyList())
        else combine(kids.map { k -> repo.observeAvailable(k.id) }) { coins ->
            kids.mapIndexed { i, k -> ProfileCard(k.id, k.name, k.grade, k.avatarColor, coins[i]) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun ProfilesScreen(onPick: (Long) -> Unit, onParent: () -> Unit, vm: ProfilesViewModel = hiltViewModel()) {
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    SkyScreen {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Leo(LeoFace.Welcome, 112.dp)
            Text("Maths Quest", style = MaterialTheme.typography.displaySmall, color = Color.White, textAlign = TextAlign.Center)
            Text("Who's playing today?", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
        WhiteCard {
            profiles.forEach { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 84.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MQ.Tint)
                        .border(2.dp, MQ.Line, RoundedCornerShape(22.dp))
                        .clickable(role = Role.Button, onClickLabel = "Play as ${p.name}") { onPick(p.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Avatar(p.name.take(1).uppercase(), p.colour)
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.headlineSmall)
                        Text("Grade ${p.grade}", style = MaterialTheme.typography.bodyMedium, color = MQ.Muted)
                    }
                    CoinPill("${p.coins}")
                }
            }
        }
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White)
                .clickable(role = Role.Button, onClick = onParent)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MQ.Ink)
            Text("Parent area", style = MaterialTheme.typography.labelLarge, color = MQ.Ink)
        }
    }
}
