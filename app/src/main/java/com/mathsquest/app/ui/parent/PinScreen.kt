package com.mathsquest.app.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.mathsquest.app.data.repo.MathsQuestRepository
import com.mathsquest.app.ui.components.Leo
import com.mathsquest.app.ui.components.LeoFace
import com.mathsquest.app.ui.components.RoundIconButton
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.theme.MQ
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinViewModel @Inject constructor(private val repo: MathsQuestRepository) : ViewModel() {
    suspend fun check(pin: String) = repo.verifyPin(pin)
}

@Composable
fun PinScreen(onBack: () -> Unit, onUnlocked: () -> Unit, vm: PinViewModel = hiltViewModel()) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun press(d: String) {
        if (pin.length >= 4) return
        error = false
        pin += d
        if (pin.length == 4) {
            val entered = pin
            scope.launch {
                if (vm.check(entered)) onUnlocked() else { error = true; pin = "" }
            }
        }
    }

    SkyScreen {
        WhiteCard(spacing = 18.dp) {
            RoundIconButton(onClick = onBack, contentDescription = "Back")
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Leo(LeoFace.Covering, 96.dp)
                Text("Parent area", style = MaterialTheme.typography.headlineMedium)
                Text("Enter your 4-digit PIN", style = MaterialTheme.typography.bodyLarge, color = MQ.Muted)
                Text("Grown-ups only. Leo promises not to peek!", style = MaterialTheme.typography.labelLarge, color = MQ.DeepOrange)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "${pin.length} of 4 digits entered" },
                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            ) {
                repeat(4) { i ->
                    Box(
                        Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (i < pin.length) MQ.Ink else Color.Transparent)
                            .border(2.dp, MQ.Ink, CircleShape),
                    )
                }
            }
            Text(
                if (error) "That PIN did not match. Try again." else "",
                color = MQ.RedText,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().height(22.dp),
            )
            val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { k ->
                        if (k.isEmpty()) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            val isBack = k == "⌫"
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(62.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isBack) Color.Transparent else MQ.Tint)
                                    .border(if (isBack) 0.dp else 2.dp, MQ.Line, RoundedCornerShape(18.dp))
                                    .clickable(role = Role.Button, onClickLabel = if (isBack) "Delete digit" else k) {
                                        if (isBack) pin = pin.dropLast(1) else press(k)
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Text(k, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = MQ.Ink) }
                        }
                    }
                }
            }
        }
    }
}
