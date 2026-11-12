package com.example.ppp.data.repository

import com.example.ppp.BuildConfig
import com.example.ppp.data.local.GameDao
import com.example.ppp.data.local.GameEntity
import com.example.ppp.data.local.KeyValueStore
import com.example.ppp.data.local.TransactionRunner
import com.example.ppp.data.model.ExploreGenre
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.GameSortOrder
import com.example.ppp.data.model.PlatformFilter
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.data.model.ServiceStatus
import com.example.ppp.data.model.playStationOnly
import com.example.ppp.data.remote.IgdbApi
import com.example.ppp.data.remote.IgdbGameDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/** Outcome of merging a backup into the library. */
data class ImportResult(val added: Int, val updated: Int, val skipped: Int)

@Suppress("TooManyFunctions")
class GameRepository(
    private val api: IgdbApi,
    private val gameDao: GameDao,
    private val store: KeyValueStore,
    private val transactions: TransactionRunner,
    private val clientId: String = BuildConfig.TWITCH_CLIENT_ID,
    private val clientSecret: String = BuildConfig.TWITCH_CLIENT_SECRET
) {
    private val _serviceStatus = MutableStateFlow<ServiceStatus?>(null)
    val serviceStatusFlow: StateFlow<ServiceStatus?> = _serviceStatus.asStateFlow()

    /** The saved library, most recently updated first. Backed by Room, so it never blocks the UI thread. */
    val savedGamesFlow: Flow<List<Game>> =
        gameDao.observeAll().map { entities -> entities.map { it.toGame() } }

    suspend fun getGameById(id: Int): Game? = gameDao.getGameById(id)?.toGame()

    suspend fun toggleFavorite(game: Game) {
        persist(game.copy(isFavorite = !game.isFavorite, updatedAt = System.currentTimeMillis()))
    }

    @Suppress("LongParameterList")
    suspend fun saveGameRating(
        game: Game,
        userRating: Float,
        status: PlayStatus,
        reviewText: String,
        hasPlatinum: Boolean = false,
        playtimeHours: Int = 0,
        isFavorite: Boolean = game.isFavorite,
        completionYear: Int? = game.completionYear
    ) {
        persist(
            game.copy(
                userRating = userRating,
                playStatus = status,
                reviewText = reviewText,
                hasPlatinum = hasPlatinum,
                playtimeHours = playtimeHours,
                isFavorite = isFavorite,
                completionYear = completionYear,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /** Games that are neither tracked nor favorited are not kept in the library. */
    private suspend fun persist(game: Game) {
        if (game.playStatus == PlayStatus.NONE && !game.isFavorite) {
            gameDao.deleteById(game.id)
        } else {
            gameDao.insertOrUpdate(GameEntity.fromGame(game))
        }
    }

    suspend fun removeGameFromLibrary(gameId: Int) {
        gameDao.deleteById(gameId)
    }

    suspend fun exportLibraryJson(): String {
        val games = gameDao.getAllSavedGames().map { it.toGame() }
        return prettyJson.encodeToString(ListSerializer(Game.serializer()), games)
    }

    /**
     * Merges a backup into the library. For games present in both, the more recently updated
     * entry wins, so importing an old backup never overwrites newer local changes.
     * Returns null when the text is not a valid, non-empty backup.
     */
    suspend fun importLibraryJson(jsonString: String): ImportResult? {
        val imported = try {
            lenientJson.decodeFromString(ListSerializer(Game.serializer()), jsonString)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (imported.isEmpty()) return null

        val existingById = gameDao.getAllSavedGames().associateBy { it.id }
        val toWrite = mutableListOf<Game>()
        var added = 0
        var updated = 0
        var skipped = 0
        for (game in imported.distinctBy { it.id }) {
            val existing = existingById[game.id]
            when {
                existing == null -> {
                    toWrite += game
                    added++
                }
                game.updatedAt > existing.updatedAt -> {
                    toWrite += game
                    updated++
                }
                else -> skipped++
            }
        }
        if (toWrite.isNotEmpty()) gameDao.insertOrUpdateAll(toWrite.map { GameEntity.fromGame(it) })

        // A backup may predate the IGDB move and carry RAWG ids, so allow the re-match to run again.
        store.putBoolean(KEY_IGDB_MIGRATION_DONE, false)
        return ImportResult(added, updated, skipped)
    }

    @Suppress("LongParameterList")
    suspend fun fetchPlayStationGames(
        query: String? = null,
        page: Int = 1,
        sortOrder: GameSortOrder = GameSortOrder.RATING_DESC,
        platform: PlatformFilter = PlatformFilter.ALL,
        genre: ExploreGenre = ExploreGenre.ALL
    ): List<Game> {
        val trimmedQuery = query?.trim()
        if (clientId.isBlank() || clientSecret.isBlank()) {
            _serviceStatus.value = ServiceStatus(
                isOutage = true,
                message = "IGDB credentials are missing. Set TWITCH_CLIENT_ID and TWITCH_CLIENT_SECRET in local.properties.",
                severity = "warning"
            )
            error("IGDB credentials are missing")
        }
        val isCacheable = page == 1 && trimmedQuery.isNullOrEmpty()
        val cacheKey = "explore_cache_${sortOrder.name}_${platform.name}_${genre.name}"

        val dtos = try {
            api.getPlayStationGames(
                clientId = clientId,
                clientSecret = clientSecret,
                searchQuery = trimmedQuery,
                page = page,
                pageSize = PAGE_SIZE,
                sortOrder = sortOrder,
                platformIds = platform.platformIds,
                genreIds = genre.genreIds
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (_serviceStatus.value == null) {
                _serviceStatus.value = ServiceStatus(
                    isOutage = true,
                    message = "IGDB API request failed (${e.localizedMessage ?: "Connection error"}). Check network or Twitch credentials.",
                    severity = "warning"
                )
            }
            val cached = if (isCacheable) readExploreCache(cacheKey) else null
            // Fall back to the last good first page; otherwise let callers see the failure.
            return cached ?: throw e
        }

        val savedById = gameDao.getAllSavedGames().associate { it.id to it.toGame() }
        if (isCacheable && dtos.isNotEmpty()) {
            writeExploreCache(cacheKey, dtos.map { it.toGame(emptyMap()) })
        }
        return dtos.map { dto -> dto.toGame(savedById) }
    }

    private suspend fun readExploreCache(key: String): List<Game>? {
        val json = store.getString(key) ?: return null
        val cached = try {
            lenientJson.decodeFromString(ListSerializer(Game.serializer()), json)
        } catch (_: SerializationException) {
            return null
        }
        if (cached.isEmpty()) return null
        val savedById = gameDao.getAllSavedGames().associate { it.id to it.toGame() }
        return cached.map { game ->
            savedById[game.id]?.takeIf { normalizeTitle(it.title) == normalizeTitle(game.title) } ?: game
        }
    }

    private fun writeExploreCache(key: String, games: List<Game>) {
        store.putString(key, Json.encodeToString(ListSerializer(Game.serializer()), games))
    }

    /** Typical time to beat in whole hours, or 0 when unknown or unavailable. */
    suspend fun fetchAveragePlaytimeHours(gameId: Int): Int {
        if (clientId.isBlank() || clientSecret.isBlank()) return 0
        return try {
            val seconds = api.getTimeToBeatSeconds(clientId, clientSecret, gameId) ?: return 0
            (seconds / SECONDS_PER_HOUR.toDouble()).roundToInt().coerceAtLeast(1)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            0
        }
    }

    /** Stores the average playtime on an already-saved game without touching its update time. */
    suspend fun cacheAveragePlaytime(gameId: Int, hours: Int) {
        val saved = gameDao.getGameById(gameId) ?: return
        if (saved.averagePlaytime != hours) gameDao.insertOrUpdate(saved.copy(averagePlaytime = hours))
    }

    /**
     * Points a saved game at a different IGDB entry, keeping the user's rating, review and status.
     * Returns false if the target is already in the library under another entry.
     */
    suspend fun relinkGame(old: Game, target: Game): Boolean {
        if (target.id == old.id) return true
        if (gameDao.getGameById(target.id) != null) return false
        replaceEntries(listOf(old.id), listOf(GameEntity.fromGame(old.relinkedTo(target))))
        return true
    }

    /**
     * One-time re-match of library entries saved with RAWG IDs onto IGDB IDs, matching by title.
     * Idempotent: an entry that already has its IGDB ID maps to itself, so a partial run can be repeated.
     * Entries with no exact title match keep their old ID and all user data.
     */
    suspend fun migrateLegacyRawgIds() {
        if (store.getBoolean(KEY_IGDB_MIGRATION_DONE)) return
        val saved = gameDao.getAllSavedGames().map { it.toGame() }
        if (saved.isEmpty()) {
            store.putBoolean(KEY_IGDB_MIGRATION_DONE, true)
            return
        }
        if (clientId.isBlank() || clientSecret.isBlank()) return

        var hadError = false
        val remapped = mutableListOf<Pair<Game, Game>>() // old -> new
        val claimedIds = saved.map { it.id }.toMutableSet()
        for (old in saved) {
            try {
                val results = api.getPlayStationGames(
                    clientId = clientId,
                    clientSecret = clientSecret,
                    searchQuery = old.title,
                    pageSize = MATCH_CANDIDATES
                )
                val match = results.firstOrNull { normalizeTitle(it.name) == normalizeTitle(old.title) }
                if (match != null && match.id != old.id && claimedIds.add(match.id)) {
                    remapped += old to old.relinkedTo(match.toGame(emptyMap()))
                }
                delay(IGDB_REQUEST_SPACING_MS.milliseconds) // IGDB allows ~4 requests/second
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                hadError = true
            }
        }

        if (remapped.isNotEmpty()) {
            replaceEntries(
                oldIds = remapped.map { (old, _) -> old.id },
                replacements = remapped.map { (_, new) -> GameEntity.fromGame(new) }
            )
        }
        if (!hadError) store.putBoolean(KEY_IGDB_MIGRATION_DONE, true)
    }

    /** Swaps entries for new ones atomically, so a failure never leaves a game deleted but not re-added. */
    private suspend fun replaceEntries(oldIds: List<Int>, replacements: List<GameEntity>) {
        transactions.run {
            gameDao.deleteByIds(oldIds)
            gameDao.insertOrUpdateAll(replacements)
        }
    }

    suspend fun checkRemoteStatus(statusUrl: String): ServiceStatus? {
        return try {
            val status = api.fetchServiceStatus(statusUrl)
            _serviceStatus.value = status
            status
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    fun dismissServiceStatus() {
        _serviceStatus.value = null
    }

    private fun IgdbGameDto.toGame(savedMap: Map<Int, Game>): Game {
        // Title check guards against an unmigrated RAWG ID colliding with an unrelated IGDB ID.
        val existingSaved = savedMap[id]?.takeIf { normalizeTitle(it.title) == normalizeTitle(name) }
        if (existingSaved != null) return existingSaved

        val coverId = cover?.imageId
        val coverUrl = if (!coverId.isNullOrEmpty()) {
            "https://images.igdb.com/igdb/image/upload/t_cover_big/$coverId.jpg"
        } else null

        val bgId = artworks?.firstOrNull()?.imageId
            ?: screenshots?.firstOrNull()?.imageId
            ?: coverId
        val backgroundUrl = if (!bgId.isNullOrEmpty()) {
            "https://images.igdb.com/igdb/image/upload/t_screenshot_huge/$bgId.jpg"
        } else null

        val rating5 = if (totalRating != null && totalRating > 0.0) totalRating / 20.0 else 0.0
        val roundedRating = (rating5 * 10).roundToInt() / 10.0

        return Game(
            id = id,
            title = name,
            coverUrl = coverUrl,
            backgroundUrl = backgroundUrl,
            releaseDate = formatReleaseDate(firstReleaseDate),
            platforms = platforms.orEmpty().map { it.name }.playStationOnly(),
            genres = genres?.map { it.name } ?: emptyList(),
            igdbRating = roundedRating,
            averagePlaytime = 0
        )
    }

    private fun formatReleaseDate(timestampSeconds: Long?): String? {
        if (timestampSeconds == null || timestampSeconds <= 0L) return null
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestampSeconds * 1000L))
    }

    companion object {
        private val prettyJson = Json { prettyPrint = true }
        private val lenientJson = Json { ignoreUnknownKeys = true }

        const val PAGE_SIZE = 20
        const val KEY_IGDB_MIGRATION_DONE = "igdb_id_migration_done"
        private const val IGDB_REQUEST_SPACING_MS = 300L
        private const val MATCH_CANDIDATES = 10
        private const val SECONDS_PER_HOUR = 3600
    }
}

/** Keeps the user's data from [this] and takes the catalog data (id, art, genres...) from [target]. */
fun Game.relinkedTo(target: Game): Game = copy(
    id = target.id,
    title = target.title,
    coverUrl = target.coverUrl ?: coverUrl,
    backgroundUrl = target.backgroundUrl ?: backgroundUrl,
    releaseDate = target.releaseDate ?: releaseDate,
    platforms = target.platforms,
    genres = target.genres.ifEmpty { genres },
    igdbRating = target.igdbRating,
    averagePlaytime = if (target.averagePlaytime > 0) target.averagePlaytime else averagePlaytime
)
