package com.example.ppp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.PlayStatus
import androidx.core.database.sqlite.transaction

class LocalGameDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_GAMES (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_COVER_URL TEXT,
                $COLUMN_BACKGROUND_URL TEXT,
                $COLUMN_RELEASE_DATE TEXT,
                $COLUMN_PLATFORMS TEXT NOT NULL,
                $COLUMN_GENRES TEXT NOT NULL DEFAULT '',
                $COLUMN_RAWG_RATING REAL NOT NULL,
                $COLUMN_USER_RATING REAL NOT NULL,
                $COLUMN_PLAY_STATUS TEXT NOT NULL,
                $COLUMN_REVIEW_TEXT TEXT NOT NULL,
                $COLUMN_HAS_PLATINUM INTEGER NOT NULL DEFAULT 0,
                $COLUMN_PLAYTIME_HOURS INTEGER NOT NULL DEFAULT 0,
                $COLUMN_AVERAGE_PLAYTIME INTEGER NOT NULL DEFAULT 0,
                $COLUMN_IS_FAVORITE INTEGER NOT NULL DEFAULT 0,
                $COLUMN_COMPLETION_YEAR INTEGER,
                $COLUMN_UPDATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_updated_at ON $TABLE_GAMES ($COLUMN_UPDATED_AT DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_play_status ON $TABLE_GAMES ($COLUMN_PLAY_STATUS)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_GAMES ADD COLUMN $COLUMN_HAS_PLATINUM INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_GAMES ADD COLUMN $COLUMN_PLAYTIME_HOURS INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_GAMES ADD COLUMN $COLUMN_GENRES TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_GAMES ADD COLUMN $COLUMN_AVERAGE_PLAYTIME INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_GAMES ADD COLUMN $COLUMN_IS_FAVORITE INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_GAMES ADD COLUMN $COLUMN_COMPLETION_YEAR INTEGER")
        }
        if (oldVersion < 5) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_updated_at ON $TABLE_GAMES ($COLUMN_UPDATED_AT DESC)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_play_status ON $TABLE_GAMES ($COLUMN_PLAY_STATUS)")
        }
    }

    fun getAllSavedGames(): List<Game> {
        val list = mutableListOf<Game>()
        val db = readableDatabase
        val cursor = db.query(TABLE_GAMES, null, null, null, null, null, "$COLUMN_UPDATED_AT DESC")
        cursor.use { c ->
            val idIdx = c.getColumnIndexOrThrow(COLUMN_ID)
            val titleIdx = c.getColumnIndexOrThrow(COLUMN_TITLE)
            val coverIdx = c.getColumnIndexOrThrow(COLUMN_COVER_URL)
            val bgIdx = c.getColumnIndexOrThrow(COLUMN_BACKGROUND_URL)
            val releaseIdx = c.getColumnIndexOrThrow(COLUMN_RELEASE_DATE)
            val platformsIdx = c.getColumnIndexOrThrow(COLUMN_PLATFORMS)
            val genresIdx = c.getColumnIndex(COLUMN_GENRES)
            val rawgIdx = c.getColumnIndexOrThrow(COLUMN_RAWG_RATING)
            val userIdx = c.getColumnIndexOrThrow(COLUMN_USER_RATING)
            val statusIdx = c.getColumnIndexOrThrow(COLUMN_PLAY_STATUS)
            val reviewIdx = c.getColumnIndexOrThrow(COLUMN_REVIEW_TEXT)
            val platIdx = c.getColumnIndex(COLUMN_HAS_PLATINUM)
            val playtimeIdx = c.getColumnIndex(COLUMN_PLAYTIME_HOURS)
            val avgPlaytimeIdx = c.getColumnIndex(COLUMN_AVERAGE_PLAYTIME)
            val favIdx = c.getColumnIndex(COLUMN_IS_FAVORITE)
            val yearIdx = c.getColumnIndex(COLUMN_COMPLETION_YEAR)
            val updatedIdx = c.getColumnIndexOrThrow(COLUMN_UPDATED_AT)

            while (c.moveToNext()) {
                val statusStr = c.getString(statusIdx)
                val status = try { PlayStatus.valueOf(statusStr) } catch (e: Exception) { PlayStatus.NONE }
                val platformsStr = c.getString(platformsIdx)
                val platforms = if (platformsStr.isBlank()) emptyList() else platformsStr.split(",")
                val genresStr = if (genresIdx != -1) c.getString(genresIdx) else ""
                val genres = if (genresStr.isBlank()) emptyList() else genresStr.split(",")
                val hasPlatinum = platIdx != -1 && c.getInt(platIdx) == 1
                val playtimeHours = if (playtimeIdx != -1) c.getInt(playtimeIdx) else 0
                val averagePlaytime = if (avgPlaytimeIdx != -1) c.getInt(avgPlaytimeIdx) else 0
                val isFavorite = favIdx != -1 && c.getInt(favIdx) == 1
                val completionYear = if (yearIdx != -1 && !c.isNull(yearIdx)) c.getInt(yearIdx) else null

                list.add(
                    Game(
                        id = c.getInt(idIdx),
                        title = c.getString(titleIdx),
                        coverUrl = c.getString(coverIdx),
                        backgroundUrl = c.getString(bgIdx),
                        releaseDate = c.getString(releaseIdx),
                        platforms = platforms,
                        genres = genres,
                        igdbRating = c.getDouble(rawgIdx),
                        userRating = c.getFloat(userIdx),
                        playStatus = status,
                        reviewText = c.getString(reviewIdx),
                        hasPlatinum = hasPlatinum,
                        playtimeHours = playtimeHours,
                        averagePlaytime = averagePlaytime,
                        isFavorite = isFavorite,
                        completionYear = completionYear,
                        updatedAt = c.getLong(updatedIdx)
                    )
                )
            }
        }
        return list
    }

    fun saveGame(game: Game) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, game.id)
            put(COLUMN_TITLE, game.title)
            put(COLUMN_COVER_URL, game.coverUrl)
            put(COLUMN_BACKGROUND_URL, game.backgroundUrl)
            put(COLUMN_RELEASE_DATE, game.releaseDate)
            put(COLUMN_PLATFORMS, game.platforms.joinToString(","))
            put(COLUMN_GENRES, game.genres.joinToString(","))
            put(COLUMN_RAWG_RATING, game.igdbRating)
            put(COLUMN_USER_RATING, game.userRating)
            put(COLUMN_PLAY_STATUS, game.playStatus.name)
            put(COLUMN_REVIEW_TEXT, game.reviewText)
            put(COLUMN_HAS_PLATINUM, if (game.hasPlatinum) 1 else 0)
            put(COLUMN_PLAYTIME_HOURS, game.playtimeHours)
            put(COLUMN_AVERAGE_PLAYTIME, game.averagePlaytime)
            put(COLUMN_IS_FAVORITE, if (game.isFavorite) 1 else 0)
            put(COLUMN_COMPLETION_YEAR, game.completionYear)
            put(COLUMN_UPDATED_AT, game.updatedAt)
        }
        db.insertWithOnConflict(TABLE_GAMES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun saveGames(games: List<Game>) {
        val db = writableDatabase
        db.transaction {
            try {
                for (game in games) {
                    val values = ContentValues().apply {
                        put(COLUMN_ID, game.id)
                        put(COLUMN_TITLE, game.title)
                        put(COLUMN_COVER_URL, game.coverUrl)
                        put(COLUMN_BACKGROUND_URL, game.backgroundUrl)
                        put(COLUMN_RELEASE_DATE, game.releaseDate)
                        put(COLUMN_PLATFORMS, game.platforms.joinToString(","))
                        put(COLUMN_GENRES, game.genres.joinToString(","))
                        put(COLUMN_RAWG_RATING, game.igdbRating)
                        put(COLUMN_USER_RATING, game.userRating)
                        put(COLUMN_PLAY_STATUS, game.playStatus.name)
                        put(COLUMN_REVIEW_TEXT, game.reviewText)
                        put(COLUMN_HAS_PLATINUM, if (game.hasPlatinum) 1 else 0)
                        put(COLUMN_PLAYTIME_HOURS, game.playtimeHours)
                        put(COLUMN_AVERAGE_PLAYTIME, game.averagePlaytime)
                        put(COLUMN_IS_FAVORITE, if (game.isFavorite) 1 else 0)
                        put(COLUMN_COMPLETION_YEAR, game.completionYear)
                        put(COLUMN_UPDATED_AT, game.updatedAt)
                    }
                    insertWithOnConflict(TABLE_GAMES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
            } finally {
            }
        }
    }

    fun deleteGame(gameId: Int) {
        val db = writableDatabase
        db.delete(TABLE_GAMES, "$COLUMN_ID = ?", arrayOf(gameId.toString()))
    }

    companion object {
        private const val DATABASE_NAME = "playstation_rater.db"
        private const val DATABASE_VERSION = 5

        private const val TABLE_GAMES = "saved_games"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_COVER_URL = "cover_url"
        private const val COLUMN_BACKGROUND_URL = "background_url"
        private const val COLUMN_RELEASE_DATE = "release_date"
        private const val COLUMN_PLATFORMS = "platforms"
        private const val COLUMN_GENRES = "genres"
        private const val COLUMN_RAWG_RATING = "rawg_rating"
        private const val COLUMN_USER_RATING = "user_rating"
        private const val COLUMN_PLAY_STATUS = "play_status"
        private const val COLUMN_REVIEW_TEXT = "review_text"
        private const val COLUMN_HAS_PLATINUM = "has_platinum"
        private const val COLUMN_PLAYTIME_HOURS = "playtime_hours"
        private const val COLUMN_AVERAGE_PLAYTIME = "average_playtime"
        private const val COLUMN_IS_FAVORITE = "is_favorite"
        private const val COLUMN_COMPLETION_YEAR = "completion_year"
        private const val COLUMN_UPDATED_AT = "updated_at"
    }
}
