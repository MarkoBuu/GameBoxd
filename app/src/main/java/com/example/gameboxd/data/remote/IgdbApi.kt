package com.example.gameboxd.data.remote

import com.example.gameboxd.data.model.GameSortOrder
import com.example.gameboxd.data.model.ServiceStatus

/** Remote game source. The repository depends on this so tests can substitute a fake. */
interface IgdbApi {
    @Suppress("LongParameterList")
    suspend fun getPlayStationGames(
        clientId: String,
        clientSecret: String,
        searchQuery: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
        sortOrder: GameSortOrder = GameSortOrder.RATING_DESC,
        platformIds: List<Int>? = null,
        genreIds: List<Int>? = null
    ): List<IgdbGameDto>

    /** Typical time to beat the game, in seconds, or null when IGDB has no data. */
    suspend fun getTimeToBeatSeconds(clientId: String, clientSecret: String, gameId: Int): Long?

    suspend fun fetchServiceStatus(statusUrl: String): ServiceStatus
}
