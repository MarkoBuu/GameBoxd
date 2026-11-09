package com.example.ppp.data.remote

import com.example.ppp.data.model.ServiceStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RawgGameListResponse(
    @SerialName("count") val count: Int,
    @SerialName("next") val next: String? = null,
    @SerialName("results") val results: List<RawgGameDto> = emptyList()
)

@Serializable
data class RawgGameDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("background_image") val backgroundImage: String? = null,
    @SerialName("released") val released: String? = null,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("playtime") val playtime: Int? = null,
    @SerialName("platforms") val platforms: List<RawgPlatformWrapper>? = null,
    @SerialName("genres") val genres: List<RawgGenreDto>? = null
)

@Serializable
data class RawgPlatformWrapper(
    @SerialName("platform") val platform: RawgPlatform
)

@Serializable
data class RawgPlatform(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String
)

@Serializable
data class RawgGenreDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("slug") val slug: String? = null
)

class RawgKtorApi(
    private val client: HttpClient = createHttpClient()
) {
    suspend fun getPlayStationGames(
        apiKey: String,
        searchQuery: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
        ordering: String? = null
    ): RawgGameListResponse {
        return client.get("https://api.rawg.io/api/games") {
            parameter("key", apiKey)
            parameter("parent_platforms", "2") // 2 = PlayStation
            parameter("page", page)
            parameter("page_size", pageSize)
            if (!searchQuery.isNullOrBlank()) {
                parameter("search", searchQuery)
                parameter("search_precise", "true")
            } else if (!ordering.isNullOrBlank()) {
                parameter("ordering", ordering)
            }
        }.body()
    }

    suspend fun fetchServiceStatus(statusUrl: String): ServiceStatus {
        return client.get(statusUrl).body()
    }

    companion object {
        fun createHttpClient(): HttpClient {
            return HttpClient(OkHttp) {
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
