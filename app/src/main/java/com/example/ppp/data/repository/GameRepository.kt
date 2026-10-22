package com.example.ppp.data.repository

import com.example.ppp.BuildConfig
import com.example.ppp.data.local.GameDao
import com.example.ppp.data.local.GameEntity
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.GameSortOrder
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.data.remote.RawgKtorApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class GameRepository(
    private val ktorApi: RawgKtorApi,
    private val gameDao: GameDao
) {
    private var apiKey: String = BuildConfig.RAWG_API_KEY

    private val _savedGamesMap = MutableStateFlow<Map<Int, Game>>(emptyMap())
    private val _savedGames = MutableStateFlow<List<Game>>(emptyList())
    val savedGamesFlow: StateFlow<List<Game>> = _savedGames.asStateFlow()

    init {
        val initialSavedList = gameDao.getAllSavedGames().map { it.toGame() }
        val initialMap = initialSavedList.associateBy { it.id }
        _savedGamesMap.value = initialMap
        _savedGames.value = initialSavedList
    }

    fun getGameById(id: Int): Game? {
        return _savedGamesMap.value[id]
    }

    fun toggleFavorite(game: Game) {
        val updated = game.copy(isFavorite = !game.isFavorite, updatedAt = System.currentTimeMillis())
        val newMap = _savedGamesMap.value.toMutableMap()
        if (updated.playStatus == PlayStatus.NONE && !updated.isFavorite) {
            newMap.remove(game.id)
            gameDao.deleteById(game.id)
        } else {
            newMap[game.id] = updated
            gameDao.insertOrUpdate(GameEntity.fromGame(updated))
        }
        _savedGamesMap.value = newMap
        _savedGames.value = newMap.values.sortedByDescending { it.updatedAt }
    }

    fun saveGameRating(
        game: Game,
        userRating: Float,
        status: PlayStatus,
        reviewText: String,
        hasPlatinum: Boolean = false,
        playtimeHours: Int = 0,
        isFavorite: Boolean = game.isFavorite,
        completionYear: Int? = game.completionYear
    ) {
        val updated = game.copy(
            userRating = userRating,
            playStatus = status,
            reviewText = reviewText,
            hasPlatinum = hasPlatinum,
            playtimeHours = playtimeHours,
            isFavorite = isFavorite,
            completionYear = completionYear,
            updatedAt = System.currentTimeMillis()
        )
        val newMap = _savedGamesMap.value.toMutableMap()
        if (status == PlayStatus.NONE && !isFavorite) {
            newMap.remove(game.id)
            gameDao.deleteById(game.id)
        } else {
            newMap[game.id] = updated
            gameDao.insertOrUpdate(GameEntity.fromGame(updated))
        }
        _savedGamesMap.value = newMap
        _savedGames.value = newMap.values.sortedByDescending { it.updatedAt }
    }

    fun removeGameFromLibrary(gameId: Int) {
        val newMap = _savedGamesMap.value.toMutableMap()
        newMap.remove(gameId)
        gameDao.deleteById(gameId)
        _savedGamesMap.value = newMap
        _savedGames.value = newMap.values.sortedByDescending { it.updatedAt }
    }

    fun exportLibraryJson(): String {
        val json = Json { prettyPrint = true }
        return json.encodeToString(ListSerializer(Game.serializer()), _savedGames.value)
    }

    fun importLibraryJson(jsonString: String): Boolean {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val importedGames = json.decodeFromString(ListSerializer(Game.serializer()), jsonString)
            if (importedGames.isEmpty()) return false
            gameDao.insertOrUpdateAll(importedGames.map { GameEntity.fromGame(it) })
            val refreshedList = gameDao.getAllSavedGames().map { it.toGame() }
            val newMap = refreshedList.associateBy { it.id }
            _savedGamesMap.value = newMap
            _savedGames.value = refreshedList
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchPlayStationGames(
        query: String? = null,
        page: Int = 1,
        sortOrder: GameSortOrder = GameSortOrder.RATING_DESC
    ): List<Game> {
        val trimmedQuery = query?.trim()?.lowercase()
        val response = try {
            val primaryResp = ktorApi.getPlayStationGames(
                apiKey = apiKey,
                searchQuery = trimmedQuery,
                page = page,
                pageSize = 20,
                ordering = sortOrder.apiValue
            )
            if (!trimmedQuery.isNullOrBlank() && primaryResp.results.isEmpty() && trimmedQuery.length in 2..3) {
                val fallbackQuery = if (trimmedQuery == "cyb") "cybe" else "${trimmedQuery}e"
                val fallbackResp = ktorApi.getPlayStationGames(
                    apiKey = apiKey,
                    searchQuery = fallbackQuery,
                    page = page,
                    pageSize = 20,
                    ordering = sortOrder.apiValue
                )
                if (fallbackResp.results.isNotEmpty()) fallbackResp else primaryResp
            } else {
                primaryResp
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        val remoteResults = response?.results ?: emptyList()
        val savedMap = _savedGamesMap.value

        return remoteResults.map { dto ->
            val existingSaved = savedMap[dto.id]
            existingSaved
                ?: Game(
                    id = dto.id,
                    title = dto.name,
                    coverUrl = dto.backgroundImage,
                    backgroundUrl = dto.backgroundImage,
                    releaseDate = dto.released,
                    platforms = dto.platforms?.map { it.platform.name } ?: listOf("PlayStation"),
                    genres = dto.genres?.map { it.name } ?: emptyList(),
                    rawgRating = dto.rating ?: 0.0,
                    averagePlaytime = dto.playtime ?: 0
                )
        }
    }

    fun matchesToken(titleLower: String, token: String): Boolean {
        val cleanTitle = titleLower.replace("[^a-z0-9\\s]".toRegex(), " ")
        if (cleanTitle.contains(token) || titleLower.contains(token)) return true

        val equivalent = when (token) {
            "1" -> "i"
            "2" -> "ii"
            "3" -> "iii"
            "4" -> "iv"
            "5" -> "v"
            "6" -> "vi"
            "7" -> "vii"
            "8" -> "viii"
            "9" -> "ix"
            "10" -> "x"
            "11" -> "xi"
            "12" -> "xii"
            "13" -> "xiii"
            "14" -> "xiv"
            "15" -> "xv"
            "16" -> "xvi"
            "17" -> "xvii"
            "18" -> "xviii"
            "19" -> "xix"
            "20" -> "xx"
            "i" -> "1"
            "ii" -> "2"
            "iii" -> "3"
            "iv" -> "4"
            "v" -> "5"
            "vi" -> "6"
            "vii" -> "7"
            "viii" -> "8"
            "ix" -> "9"
            "x" -> "10"
            "xi" -> "11"
            "xii" -> "12"
            "xiii" -> "13"
            "xiv" -> "14"
            "xv" -> "15"
            "xvi" -> "16"
            "xvii" -> "17"
            "xviii" -> "18"
            "xix" -> "19"
            "xx" -> "20"
            else -> null
        }
        return equivalent != null && (cleanTitle.contains(equivalent) || titleLower.contains(equivalent))
    }
}
