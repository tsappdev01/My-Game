package com.mathsquest.app.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.mathsquest.app.data.repo.MathsQuestRepository
import com.mathsquest.app.ui.components.ChunkyButton
import com.mathsquest.app.ui.components.LeoFace
import com.mathsquest.app.ui.components.LeoSays
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.theme.MQ
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(private val repo: MathsQuestRepository) : ViewModel() {
    suspend fun complete(pin: String, name: String, grade: Int, cap: Int) = repo.completeSetup(pin, name, grade, cap)
}

/** First run: a parent gives consent, sets a PIN and adds the first child. */
@Composable
fun SetupScreen(onDone: () -> Unit, vm: SetupViewModel = hiltViewModel()) {
    var consent by rememberSaveable { mutableStateOf(false) }
    var pin by rememberSaveable { mutableStateOf("") }
    var pin2 by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var grade by rememberSaveable { mutableIntStateOf(5) }
    var cap by rememberSaveable { mutableIntStateOf(MathsQuestRepository.DEFAULT_CAP) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val pinOk = pin.length == 4 && pin == pin2
    val ready = consent && pinOk && name.isNotBlank() && !saving

    SkyScreen {
        Text("Welcome, grown-up!", style = MaterialTheme.typography.headlineMedium, color = Color.White, modifier = Modifier.fillMaxWidth())
        WhiteCard {
            LeoSays(LeoFace.Reporter, "Let's set things up. This takes about a minute.", label = "Parent setup", bob = false)
            Text("1. Your consent", style = MaterialTheme.typography.titleLarge)
            Text(
                "Maths Quest stores your child's first name, grade and practice results on this device only. " +
                    "There are no ads, no tracking and no chat. You can delete everything by uninstalling the app.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = consent, onCheckedChange = { consent = it })
                Text("I'm the parent or guardian and I agree.", style = MaterialTheme.typography.bodyLarge)
            }
        }
        WhiteCard {
            Text("2. Parent PIN", style = MaterialTheme.typography.titleLarge)
            Text("You'll need it to approve rewards and open the parent dashboard.", style = MaterialTheme.typography.bodyMedium, color = MQ.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PinField("4-digit PIN", pin, { pin = it }, Modifier.weight(1f))
                PinField("Repeat PIN", pin2, { pin2 = it }, Modifier.weight(1f))
            }
            if (pin2.length == 4 && pin != pin2) Text("The PINs don't match.", color = MQ.RedText, style = MaterialTheme.typography.bodyMedium)
        }
        WhiteCard {
            Text("3. Add your child", style = MaterialTheme.typography.titleLarge)
            ChildFormFields(name, { name = it }, grade, { grade = it }, cap, { cap = it })
        }
        ChunkyButton(
            text = "Start Maths Quest",
            color = MQ.Green,
            enabled = ready,
            onClick = {
                saving = true
                scope.launch {
                    vm.complete(pin, name, grade, cap)
                    onDone()
                }
            },
        )
    }
}

@Composable
fun PinField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValue(v.filter(Char::isDigit).take(4)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier,
    )
}
