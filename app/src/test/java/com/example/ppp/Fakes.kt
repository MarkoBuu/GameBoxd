package com.example.ppp

import com.example.ppp.data.local.GameDao
import com.example.ppp.data.local.GameEntity
import com.example.ppp.data.local.KeyValueStore
import com.example.ppp.data.local.TransactionRunner
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.GameSortOrder
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.data.model.ServiceStatus
import com.example.ppp.data.remote.IgdbApi
import com.example.ppp.data.remote.IgdbGameDto
import com.example.ppp.data.remote.IgdbImageDto
import com.example.ppp.data.remote.IgdbNamedDto
import com.example.ppp.data.repository.GameRepository
import com.example.ppp.data.repository.normalizeTitle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeGameDao(initial: List<Game> = emptyList()) : GameDao {
    private val state = MutableStateFlow(initial.map { GameEntity.fromGame(it) })

    val games: List<Game> get() = state.value.map { it.toGame() }.sortedBy { it.id }

    override fun observeAll(): Flow<List<GameEntity>> =
        state.map { list -> list.sortedByDescending { it.updatedAt } }

    override suspend fun getAllSavedGames(): List<GameEntity> =
        state.value.sortedByDescending { it.updatedAt }

    override suspend fun getGameById(id: Int): GameEntity? = state.value.firstOrNull { it.id == id }

    override suspend fun insertOrUpdate(game: GameEntity) {
        state.value = state.value.filterNot { it.id == game.id } + game
    }

    override suspend fun insertOrUpdateAll(games: List<GameEntity>) = games.forEach { insertOrUpdate(it) }

    override suspend fun deleteById(id: Int) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun deleteByIds(ids: List<Int>) {
        state.value = state.value.filterNot { it.id in ids }
    }
}

class InMemoryStore : KeyValueStore {
    private val booleans = mutableMapOf<String, Boolean>()
    private val strings = mutableMapOf<String, String>()
    override fun getBoolean(key: String, default: Boolean) = booleans[key] ?: default
    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }
    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String) {
        strings[key] = value
    }
}

object DirectTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}

/** Serves [catalog]; a search query filters it loosely by title. */
class FakeIgdbApi(var catalog: List<IgdbGameDto> = emptyList()) : IgdbApi {
    var failure: Exception? = null
    var timeToBeatSeconds: Long? = null
    var requestCount = 0
        private set
    var lastPlatformIds: List<Int>? = null
        private set
    var lastGenreIds: List<Int>? = null
        private set

    override suspend fun getPlayStationGames(
        clientId: String,
        clientSecret: String,
        searchQuery: String?,
        page: Int,
        pageSize: Int,
        sortOrder: GameSortOrder,
        platformIds: List<Int>?,
        genreIds: List<Int>?
    ): List<IgdbGameDto> {
        requestCount++
        lastPlatformIds = platformIds
        lastGenreIds = genreIds
        failure?.let { throw it }
        val matching = if (searchQuery.isNullOrBlank()) {
            catalog
        } else {
            // Real IGDB search is fuzzy; approximate it with a loose, accent-insensitive contains.
            catalog.filter { normalizeTitle(it.name).contains(normalizeTitle(searchQuery)) }
        }
        return matching.drop((page - 1) * pageSize).take(pageSize)
    }

    override suspend fun getTimeToBeatSeconds(clientId: String, clientSecret: String, gameId: Int): Long? {
        failure?.let { throw it }
        return timeToBeatSeconds
    }

    override suspend fun fetchServiceStatus(statusUrl: String): ServiceStatus = ServiceStatus()
}

fun dto(
    id: Int,
    name: String,
    platforms: List<String> = listOf("PlayStation 4"),
    genres: List<String> = listOf("Adventure"),
    rating: Double = 80.0
) = IgdbGameDto(
    id = id,
    name = name,
    cover = IgdbImageDto(imageId = "cover$id"),
    platforms = platforms.mapIndexed { index, platform -> IgdbNamedDto(index, platform) },
    genres = genres.mapIndexed { index, genre -> IgdbNamedDto(index, genre) },
    totalRating = rating
)

@Suppress("LongParameterList")
fun savedGame(
    id: Int,
    title: String,
    status: PlayStatus = PlayStatus.COMPLETED,
    rating: Float = 4f,
    favorite: Boolean = false,
    updatedAt: Long = 1_000L,
    genres: List<String> = listOf("Adventure")
) = Game(
    id = id,
    title = title,
    userRating = rating,
    playStatus = status,
    reviewText = "review of $title",
    hasPlatinum = true,
    playtimeHours = 30,
    isFavorite = favorite,
    completionYear = 2022,
    genres = genres,
    platforms = listOf("PlayStation 4"),
    updatedAt = updatedAt
)

fun repository(
    api: FakeIgdbApi = FakeIgdbApi(),
    dao: FakeGameDao = FakeGameDao(),
    store: InMemoryStore = InMemoryStore(),
    clientId: String = "id",
    clientSecret: String = "secret"
) = GameRepository(api, dao, store, DirectTransactionRunner, clientId, clientSecret)
