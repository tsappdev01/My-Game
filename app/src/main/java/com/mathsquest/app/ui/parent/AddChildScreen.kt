package com.mathsquest.app.ui.parent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.mathsquest.app.data.repo.MathsQuestRepository
import com.mathsquest.app.ui.components.ChunkyButton
import com.mathsquest.app.ui.components.ScreenHeader
import com.mathsquest.app.ui.components.SkyScreen
import com.mathsquest.app.ui.components.WhiteCard
import com.mathsquest.app.ui.theme.MQ
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddChildViewModel @Inject constructor(private val repo: MathsQuestRepository) : ViewModel() {
    fun add(name: String, grade: Int, cap: Int, onDone: () -> Unit) {
        viewModelScope.launch { repo.addChild(name, grade, cap); onDone() }
    }
}

@Composable
fun AddChildScreen(onDone: () -> Unit, vm: AddChildViewModel = hiltViewModel()) {
    var name by rememberSaveable { mutableStateOf("") }
    var grade by rememberSaveable { mutableIntStateOf(5) }
    var cap by rememberSaveable { mutableIntStateOf(MathsQuestRepository.DEFAULT_CAP) }
    var saving by rememberSaveable { mutableStateOf(false) }
    SkyScreen {
        WhiteCard {
            ScreenHeader("Add a child", onBack = onDone)
            ChildFormFields(name, { name = it }, grade, { grade = it }, cap, { cap = it })
        }
        ChunkyButton("Add child", MQ.Green, enabled = name.isNotBlank() && !saving, onClick = {
            saving = true
            vm.add(name, grade, cap, onDone)
        })
    }
}
