package com.example.gameboxd.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [GameEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        /** Schema versions from before migrations were maintained; these can only be reset. */
        val UNMIGRATABLE_VERSIONS = intArrayOf(1, 2, 3, 4)

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_games RENAME TO saved_games_old")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `saved_games` (
                        `id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `cover_url` TEXT,
                        `background_url` TEXT,
                        `release_date` TEXT,
                        `platforms` TEXT NOT NULL,
                        `genres` TEXT NOT NULL,
                        `rawg_rating` REAL NOT NULL,
                        `user_rating` REAL NOT NULL,
                        `play_status` TEXT NOT NULL,
                        `review_text` TEXT NOT NULL,
                        `has_platinum` INTEGER NOT NULL,
                        `playtime_hours` INTEGER NOT NULL,
                        `average_playtime` INTEGER NOT NULL,
                        `is_favorite` INTEGER NOT NULL,
                        `completion_year` INTEGER,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO saved_games (
                        id, title, cover_url, background_url, release_date,
                        platforms, genres, rawg_rating, user_rating, play_status,
                        review_text, has_platinum, playtime_hours, average_playtime,
                        is_favorite, completion_year, updated_at
                    )
                    SELECT
                        id, title, cover_url, background_url, release_date,
                        platforms, COALESCE(genres, ''), rawg_rating, user_rating, play_status,
                        review_text, COALESCE(has_platinum, 0), COALESCE(playtime_hours, 0), COALESCE(average_playtime, 0),
                        COALESCE(is_favorite, 0), completion_year, updated_at
                    FROM saved_games_old
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS saved_games_old")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_games_updated_at` ON `saved_games` (`updated_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_games_play_status` ON `saved_games` (`play_status`)")
            }
        }
    }
}
