package com.example.ppp.data.remote

import com.example.ppp.data.model.GameSortOrder
import com.example.ppp.data.model.ServiceStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TwitchAuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null
)

@Serializable
data class IgdbGameDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("cover") val cover: IgdbImageDto? = null,
    @SerialName("artworks") val artworks: List<IgdbImageDto>? = null,
    @SerialName("screenshots") val screenshots: List<IgdbImageDto>? = null,
    @SerialName("first_release_date") val firstReleaseDate: Long? = null,
    @SerialName("platforms") val platforms: List<IgdbNamedDto>? = null,
    @SerialName("genres") val genres: List<IgdbNamedDto>? = null,
    @SerialName("total_rating") val totalRating: Double? = null,
    @SerialName("summary") val summary: String? = null
)

@Serializable
data class IgdbImageDto(
    @SerialName("id") val id: Int? = null,
    @SerialName("image_id") val imageId: String? = null
)

@Serializable
data class IgdbNamedDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String
)

@Serializable
data class IgdbTimeToBeatDto(
    @SerialName("game_id") val gameId: Int,
    @SerialName("hastily") val hastily: Long? = null,
    @SerialName("normally") val normally: Long? = null,
    @SerialName("completely") val completely: Long? = null
)

class IgdbKtorApi(
    private val client: HttpClient = createHttpClient()
) : IgdbApi {
    private var cachedToken: String? = null
    private var tokenExpirationTime: Long = 0L

    suspend fun getAccessToken(clientId: String, clientSecret: String): String {
        val currentTime = System.currentTimeMillis()
        val currentCachedToken = cachedToken
        if (!currentCachedToken.isNullOrEmpty() && currentTime < tokenExpirationTime) {
            return currentCachedToken
        }
        val response: TwitchAuthResponse = client.post("https://id.twitch.tv/oauth2/token") {
            parameter("client_id", clientId)
            parameter("client_secret", clientSecret)
            parameter("grant_type", "client_credentials")
        }.body()
        cachedToken = response.accessToken
        tokenExpirationTime = currentTime + ((response.expiresIn ?: 3600L) - 300L) * 1000L
        return response.accessToken
    }

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
        val query = IgdbQueries.games(
            searchQuery = searchQuery,
            page = page,
            pageSize = pageSize,
            sortOrder = sortOrder,
            platformIds = platformIds,
            genreIds = genreIds,
            nowSeconds = System.currentTimeMillis() / 1000L
        )
        return post("games", clientId, clientSecret, query)
    }

    override suspend fun getTimeToBeatSeconds(
        clientId: String,
        clientSecret: String,
        gameId: Int
    ): Long? {
        val result: List<IgdbTimeToBeatDto> =
            post("game_time_to_beats", clientId, clientSecret, IgdbQueries.timeToBeat(gameId))
        val entry = result.firstOrNull() ?: return null
        return (entry.normally ?: entry.hastily ?: entry.completely)?.takeIf { it > 0 }
    }

    override suspend fun fetchServiceStatus(statusUrl: String): ServiceStatus {
        return client.get(statusUrl).body()
    }

    private suspend inline fun <reified T> post(
        endpoint: String,
        clientId: String,
        clientSecret: String,
        query: String
    ): T {
        val token = getAccessToken(clientId, clientSecret)
        return try {
            client.post("https://api.igdb.com/v4/$endpoint") {
                header("Client-ID", clientId)
                header("Authorization", "Bearer $token")
                setBody(query)
            }.body()
        } catch (e: ClientRequestException) {
            // Token revoked or expired early: force a fresh one on the next call.
            if (e.response.status == HttpStatusCode.Unauthorized) cachedToken = null
            throw e
        }
    }

    companion object {
        fun createHttpClient(): HttpClient {
            return HttpClient(OkHttp) {
                expectSuccess = true
                install(HttpTimeout) {
                    requestTimeoutMillis = 15000
                    connectTimeoutMillis = 15000
                    socketTimeoutMillis = 15000
                }
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
            }
        }
    }
}
