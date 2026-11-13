package com.example.gameboxd.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface MainScreens : NavKey {
    @Serializable
    data object Explore : MainScreens

    @Serializable
    data object Library: MainScreens

    @Serializable
    data object  Stats : MainScreens
}

data class TabInfo(
    val route: MainScreens,
    val title: String,
    val icon: ImageVector
)