package com.mathsquest.app.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mathsquest.app.R
import com.mathsquest.app.data.repo.MathsQuestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(repo: MathsQuestRepository) : ViewModel() {
    val isSetUp: StateFlow<Boolean?> = repo.isSetUp.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

/** The poster artwork with its own "Get Started" button: a tap anywhere starts the app. */
@Composable
fun SplashScreen(onStart: (setUp: Boolean) -> Unit, vm: SplashViewModel = hiltViewModel()) {
    val setUp by vm.isSetUp.collectAsStateWithLifecycle()
    Box(
        Modifier
            .fillMaxSize()
            .clickable(enabled = setUp != null, role = Role.Button, onClickLabel = "Get Started") { onStart(setUp == true) },
    ) {
        Image(
            painter = painterResource(R.drawable.splash_poster),
            contentDescription = "Maths Quest. Learn, Play, Earn. Get Started.",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
