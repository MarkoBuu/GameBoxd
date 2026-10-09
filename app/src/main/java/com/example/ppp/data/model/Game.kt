package com.example.ppp.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

enum class PlayStatus(val displayName: String) {
    COMPLETED("Completed"),
    BACKLOG("Backlog"),
    DROPPED("Dropped"),
    NONE("Not Saved");

    val allowsRating: Boolean
        get() = this == COMPLETED
}

@Immutable
@Serializable
data class Game(
    val id: Int,
    val title: String,
    val coverUrl: String? = null,
    val backgroundUrl: String? = null,
    val releaseDate: String? = null,
    val platforms: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val rawgRating: Double = 0.0,
    val userRating: Float = 0f, // 0.0 to 5.0 stars
    val playStatus: PlayStatus = PlayStatus.NONE,
    val reviewText: String = "",
    val hasPlatinum: Boolean = false,
    val playtimeHours: Int = 0,
    val averagePlaytime: Int = 0,
    val isFavorite: Boolean = false,
    val completionYear: Int? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
