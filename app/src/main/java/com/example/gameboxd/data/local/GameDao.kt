package com.example.gameboxd.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM saved_games ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT * FROM saved_games ORDER BY updated_at DESC")
    suspend fun getAllSavedGames(): List<GameEntity>

    @Query("SELECT * FROM saved_games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Int): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(games: List<GameEntity>)

    @Query("DELETE FROM saved_games WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM saved_games WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)
}
