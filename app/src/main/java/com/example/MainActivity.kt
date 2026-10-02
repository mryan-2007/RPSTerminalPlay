package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TerminalScreen
import com.example.ui.theme.RPSBattleTerminalTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RPSBattleTerminalTheme {
                val viewModel: GameViewModel = viewModel()
                TerminalScreen(viewModel = viewModel)
            }
        }
    }
}
