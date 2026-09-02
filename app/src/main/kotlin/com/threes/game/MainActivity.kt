package com.threes.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.threes.game.ui.screens.GameScreen
import com.threes.game.ui.theme.ThreesTheme
import com.threes.game.ui.viewmodel.ThreesViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ThreesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val uiState by viewModel.uiState.collectAsState()

            ThreesTheme(darkTheme = uiState.isNightMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}
