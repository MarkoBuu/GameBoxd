package com.example.ppp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Explore : Screen("explore", "Explore", Icons.Default.Explore)
    object Library : Screen("library", "Library", Icons.Default.VideogameAsset)
    object Stats : Screen("stats", "Stats", Icons.Default.BarChart)
}