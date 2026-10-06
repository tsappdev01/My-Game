package com.mathsquest.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mathsquest.app.ui.MathsQuestNavHost
import com.mathsquest.app.ui.theme.MathsQuestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MathsQuestTheme {
                MathsQuestNavHost()
            }
        }
    }
}
