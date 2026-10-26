package com.example.ppp.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.PlayStatus

@Entity(
    tableName = "saved_games",
    indices = [
        Index(value = ["updated_at"]),
        Index(value = ["play_status"])
    ]
)
data class GameEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "cover_url") val coverUrl: String? = null,
    @ColumnInfo(name = "background_url") val backgroundUrl: String? = null,
    @ColumnInfo(name = "release_date") val releaseDate: String? = null,
    @ColumnInfo(name = "platforms") val platforms: String = "",
    @ColumnInfo(name = "genres") val genres: String = "",
    @ColumnInfo(name = "rawg_rating") val rawgRating: Double = 0.0,
    @ColumnInfo(name = "user_rating") val userRating: Float = 0f,
    @ColumnInfo(name = "play_status") val playStatus: String = PlayStatus.NONE.name,
    @ColumnInfo(name = "review_text") val reviewText: String = "",
    @ColumnInfo(name = "has_platinum") val hasPlatinum: Boolean = false,
    @ColumnInfo(name = "playtime_hours") val playtimeHours: Int = 0,
    @ColumnInfo(name = "average_playtime") val averagePlaytime: Int = 0,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "completion_year") val completionYear: Int? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun toGame(): Game {
        val status = try { PlayStatus.valueOf(playStatus) } catch (_: Exception) { PlayStatus.NONE }
        val scaledRating = if (userRating in 0.1f..5.0f) userRating * 2f else userRating
        return Game(
            id = id,
            title = title,
            coverUrl = coverUrl,
            backgroundUrl = backgroundUrl,
            releaseDate = releaseDate,
            platforms = if (platforms.isBlank()) emptyList() else platforms.split(","),
            genres = if (genres.isBlank()) emptyList() else genres.split(","),
            rawgRating = rawgRating,
            userRating = scaledRating,
            playStatus = status,
            reviewText = reviewText,
            hasPlatinum = hasPlatinum,
            playtimeHours = playtimeHours,
            averagePlaytime = averagePlaytime,
            isFavorite = isFavorite,
            completionYear = completionYear,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromGame(game: Game): GameEntity {
            return GameEntity(
                id = game.id,
                title = game.title,
                coverUrl = game.coverUrl,
                backgroundUrl = game.backgroundUrl,
                releaseDate = game.releaseDate,
                platforms = game.platforms.joinToString(","),
                genres = game.genres.joinToString(","),
                rawgRating = game.rawgRating,
                userRating = game.userRating,
                playStatus = game.playStatus.name,
                reviewText = game.reviewText,
                hasPlatinum = game.hasPlatinum,
                playtimeHours = game.playtimeHours,
                averagePlaytime = game.averagePlaytime,
                isFavorite = game.isFavorite,
                completionYear = game.completionYear,
                updatedAt = game.updatedAt
            )
        }
    }
}
