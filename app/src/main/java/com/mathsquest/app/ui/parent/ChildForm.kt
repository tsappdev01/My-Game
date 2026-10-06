package com.mathsquest.app.ui.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathsquest.app.data.repo.MathsQuestRepository
import com.mathsquest.core.CoinRules
import com.mathsquest.core.Grades
import com.mathsquest.app.ui.theme.MQ

/** Name, grade and monthly coin limit for a child. Used by first-run setup and "Add child". */
@Composable
fun ChildFormFields(
    name: String,
    onName: (String) -> Unit,
    grade: Int,
    onGrade: (Int) -> Unit,
    cap: Int,
    onCap: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { onName(it.take(20)) },
            label = { Text("Child's first name or nickname") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Grade", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Grades.all.forEach { g ->
                val on = g == grade
                Box(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) MQ.Blue else Color.White)
                        .border(2.dp, if (on) MQ.Blue else MQ.Line, RoundedCornerShape(14.dp))
                        .clickable(role = Role.RadioButton) { onGrade(g) }
                        .semantics { selected = on; contentDescription = "Grade $g" },
                    contentAlignment = Alignment.Center,
                ) { Text("$g", color = if (on) Color.White else MQ.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
        }
        Text("Monthly coin limit", style = MaterialTheme.typography.titleMedium)
        CapStepper(cap, onCap)
        Text(
            "Coins stop for the month at this limit. 1 coin = AED 0.10 of reward value. You can change it any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MQ.Muted,
        )
    }
}

@Composable
fun CapStepper(cap: Int, onCap: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepButton("−", "Lower the limit by 50 coins") { onCap((cap - 50).coerceAtLeast(MathsQuestRepository.MIN_CAP)) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
            Text("$cap coins", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MQ.Ink)
            Text(CoinRules.aed(cap), fontSize = 12.sp, color = MQ.Muted)
        }
        StepButton("+", "Raise the limit by 50 coins") { onCap((cap + 50).coerceAtMost(MathsQuestRepository.MAX_CAP)) }
    }
}

@Composable
private fun StepButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(2.dp, Color(0xFFCFDCEE), CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Text(symbol, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MQ.Ink) }
}
