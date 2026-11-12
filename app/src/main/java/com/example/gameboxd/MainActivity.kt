package com.example.gameboxd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.gameboxd.ui.navigation.MainNavigation
import com.example.gameboxd.ui.theme.PlayStationRaterTheme
import com.example.gameboxd.ui.viewmodel.GameViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: GameViewModel = koinViewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            PlayStationRaterTheme(darkTheme = isDarkTheme) {
                MainNavigation(viewModel = viewModel)
            }
        }
    }
}
