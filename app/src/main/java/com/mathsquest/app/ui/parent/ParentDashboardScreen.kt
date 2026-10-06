package com.mathsquest.app.ui.parent

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mathsquest.app.data.local.ChildEntity
import com.mathsquest.app.data.local.CoinTransactionEntity
import com.mathsquest.app.data.local.RewardRequestEntity
import com.mathsquest.app.data.repo.MathsQuestRepository
import com.mathsquest.app.data.repo.WeekSummary
import com.mathsquest.app.ui.components.ChunkyButton
import com.mathsquest.app.ui.components.Leo
import com.mathsquest.app.ui.components.LeoFace
import com.mathsquest.app.ui.components.OutlineButton
import com.mathsquest.app.ui.components.ProgressBar
import com.mathsquest.app.ui.components.Tag
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.theme.MQ
import com.mathsquest.core.CoinRules
import com.mathsquest.core.Operation
import com.mathsquest.core.TopicStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ChildReport(
    val child: ChildEntity,
    val week: WeekSummary,
    val ops: Map<Operation, TopicStats>,
    val earnedThisMonth: Int,
    val balance: Int,
    val held: Int,
    val ledger: List<CoinTransactionEntity>,
    val streak: Int,
)

data class ParentState(
    val children: List<ChildEntity> = emptyList(),
    val selectedId: Long? = null,
    val pending: List<Pair<RewardRequestEntity, String>> = emptyList(),
    val report: ChildReport? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ParentDashboardViewModel @Inject constructor(private val repo: MathsQuestRepository) : ViewModel() {
    private val selected = MutableStateFlow<Long?>(null)

    private val report = combine(repo.observeChildren(), selected) { kids, sel -> kids.firstOrNull { it.id == sel } ?: kids.firstOrNull() }
        .flatMapLatest { child ->
            if (child == null) flowOf(null)
            else combine(
                repo.observeChild(child.id),
                repo.observeWeek(child.id),
                repo.observeOperationStats(child.id),
                repo.observeEarnedThisMonth(child.id),
                combine(repo.observeBalance(child.id), repo.observeHeld(child.id), repo.observeLedger(child.id, 8)) { b, h, l -> Triple(b, h, l) },
            ) { c, week, ops, earned, (balance, held, ledger) ->
                val live = c ?: child
                ChildReport(live, week, ops, earned, balance, held, ledger, repo.displayedStreak(live))
            }
        }

    val state: StateFlow<ParentState> = combine(repo.observeChildren(), selected, repo.observePendingRequests(), report) { kids, sel, pending, rep ->
        val names = kids.associate { it.id to it.name }
        ParentState(
            children = kids,
            selectedId = rep?.child?.id ?: sel,
            pending = pending.map { it to (names[it.childId] ?: "") },
            report = rep,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ParentState())

    fun select(id: Long) { selected.value = id }
    fun decide(requestId: Long, approve: Boolean) = viewModelScope.launch { repo.decide(requestId, approve) }
    fun setCap(childId: Long, cap: Int) = viewModelScope.launch { repo.setMonthlyCap(childId, cap) }
}

@Composable
fun ParentDashboardScreen(onExit: () -> Unit, onAddChild: () -> Unit, vm: ParentDashboardViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    SkyScreen {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Parent Dashboard", style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.weight(1f))
            OutlineButton("Exit", onClick = onExit)
        }

        WhiteCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Reward requests", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Tag("${s.pending.size}", if (s.pending.isEmpty()) Color(0xFFE3EAF4) else MQ.Red, if (s.pending.isEmpty()) MQ.Muted else Color.White)
            }
            if (s.pending.isEmpty()) Text("No requests waiting.", style = MaterialTheme.typography.bodyMedium, color = MQ.Muted)
            s.pending.forEach { (req, name) ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MQ.Tint).border(1.dp, MQ.Line, RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("$name asked for ${req.title.lowercase()}", style = MaterialTheme.typography.titleMedium)
                    Text("${req.cost} coins · ${CoinRules.aed(req.cost)} of reward value", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2F3F66))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton("Decline", onClick = { vm.decide(req.id, false) }, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.weight(1f).heightIn(min = 48.dp).clip(RoundedCornerShape(999.dp)).background(MQ.Blue)
                                .clickable(role = Role.Button) { vm.decide(req.id, true) },
                            contentAlignment = Alignment.Center,
                        ) { Text("Approve", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(Color(0x8CFFFFFF)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            s.children.forEach { c ->
                val on = c.id == s.selectedId
                Box(
                    Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(999.dp)).background(if (on) MQ.Blue else Color.Transparent)
                        .clickable(role = Role.Tab) { vm.select(c.id) },
                    contentAlignment = Alignment.Center,
                ) { Text("${c.name} · Grade ${c.grade}", color = if (on) Color.White else MQ.Ink, style = MaterialTheme.typography.labelLarge) }
            }
        }

        s.report?.let { r -> Report(r, onCap = { vm.setCap(r.child.id, it) }) }

        ChunkyButton("Add a child", MQ.Green, onClick = onAddChild, height = 54.dp, fontSize = 18)
        Text(
            "Your child only ever sees Gold Coins. 1 coin = AED 0.10 of reward value, and nothing is redeemed without your approval.",
            style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2F3F66), modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun Report(r: ChildReport, onCap: (Int) -> Unit) {
    val name = r.child.name
    val tracked = r.ops.filterValues { it.answered >= 3 }
    val best = tracked.maxByOrNull { it.value.accuracy }
    val worst = tracked.minByOrNull { it.value.accuracy }
    val note = "$name answered ${r.week.answered} questions this week and got ${r.week.accuracyPercent}% right first time." +
        (best?.let { " Strongest: ${it.key.label} (${it.value.percent}%)." } ?: "") +
        (if (worst != null && worst.value.accuracy < 0.7f) " Worth practising together: ${worst.key.label} (${worst.value.percent}%)." else " No weak spots this week.")

    WhiteCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Leo(LeoFace.Reporter, 68.dp, bob = false)
            Column(Modifier.weight(1f)) {
                Text("Leo's weekly note", color = MQ.DeepOrange, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    WhiteCard(spacing = 8.dp) {
        Text("This week", style = MaterialTheme.typography.titleLarge)
        val mins = (r.week.studyMillis / 60_000).toInt()
        listOf(
            Triple("Questions answered", "${r.week.answered}", MQ.Blue),
            Triple("Right first time", "${r.week.accuracyPercent}%", MQ.Green),
            Triple("Study time", "${mins / 60}h ${mins % 60}m", MQ.Purple),
            Triple("Coins earned", "${r.week.coinsEarned} (${CoinRules.aed(r.week.coinsEarned)})", MQ.Gold),
            Triple("Current streak", "${r.streak} ${if (r.streak == 1) "day" else "days"}", MQ.Orange),
        ).forEach { (label, value, dot) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
                Text(label, style = MaterialTheme.typography.bodyLarge, color = Color(0xFF2F3F66), modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    WhiteCard {
        Text("Accuracy by topic", style = MaterialTheme.typography.titleLarge)
        Operation.entries.forEach { op ->
            val t = r.ops[op] ?: TopicStats(0, 0)
            val strong = t.answered >= 5 && t.accuracy >= 0.85f
            val weak = t.answered >= 3 && t.accuracy < 0.7f
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(op.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (strong) Tag("Strong", MQ.GreenTint, MQ.GreenText)
                    if (weak) Tag("Needs practice", MQ.RedTint, MQ.RedText)
                    Text(if (t.answered == 0) "–" else "${t.percent}%", style = MaterialTheme.typography.titleMedium)
                }
                ProgressBar(t.accuracy, MQ.Blue, height = 8.dp, track = Color(0xFFE3EDF9))
                Text("${t.correctFirstTry} of ${t.answered} right", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    WhiteCard(spacing = 10.dp) {
        val cap = r.child.monthlyCapCoins
        val used = r.earnedThisMonth.coerceAtMost(cap)
        Text("Monthly coin limit", style = MaterialTheme.typography.titleLarge)
        Text("$name has earned $used of $cap coins this month (${CoinRules.aed(used)} of ${CoinRules.aed(cap)}).", style = MaterialTheme.typography.bodyMedium)
        ProgressBar(used / cap.toFloat(), MQ.Gold, track = Color(0xFFE3EDF9))
        CapStepper(cap, onCap)
        Text("When the limit is reached, coins stop for the month. Practice and progress tracking keep working.", style = MaterialTheme.typography.labelMedium)
    }

    WhiteCard(spacing = 4.dp) {
        Text("Coin activity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 6.dp))
        if (r.ledger.isEmpty()) Text("No coins yet.", style = MaterialTheme.typography.bodyMedium, color = MQ.Muted)
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        r.ledger.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(e.reference, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(Instant.ofEpochMilli(e.createdDate).atZone(ZoneId.systemDefault()).format(fmt), style = MaterialTheme.typography.labelMedium)
                Text(
                    (if (e.coins > 0) "+" else "−") + kotlin.math.abs(e.coins),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (e.coins > 0) MQ.GreenText else MQ.Ink,
                )
            }
        }
        Text(
            "Balance: ${r.balance} coins (${CoinRules.aed(r.balance)})" + if (r.held > 0) ", ${r.held} on hold for requests" else "",
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp),
        )
    }
}
