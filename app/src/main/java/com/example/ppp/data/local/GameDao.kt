package com.example.ppp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GameDao {
    @Query("SELECT * FROM saved_games ORDER BY updated_at DESC")
    fun getAllSavedGames(): List<GameEntity>

    @Query("SELECT * FROM saved_games WHERE id = :id LIMIT 1")
    fun getGameById(id: Int): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateAll(games: List<GameEntity>)

    @Query("DELETE FROM saved_games WHERE id = :id")
    fun deleteById(id: Int)
}
