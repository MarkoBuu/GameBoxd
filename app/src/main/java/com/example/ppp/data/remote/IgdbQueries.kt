package com.example.ppp.data.remote

import com.example.ppp.data.model.GameSortOrder

/** Builds IGDB Apicalypse query bodies. Kept free of Android/Ktor types so it is unit-testable. */
object IgdbQueries {
    // PS1, PS2, PS3, PSP, Vita, PS4, PS VR, PS5, PS VR2.
    val PLAYSTATION_PLATFORM_IDS = listOf(7, 8, 9, 38, 46, 48, 165, 167, 390)

    private const val GAME_FIELDS =
        "id, name, cover.image_id, artworks.image_id, screenshots.image_id, " +
            "first_release_date, platforms.name, genres.name, total_rating, summary"

    // Excludes DLC (1), mods (5), packs (13) and updates (14).
    private const val EXCLUDED_GAME_TYPES = "game_type != (1, 5, 13, 14)"

    private const val MIN_RATING_COUNT = 10

    // In IGDB, "( )" means "any of" and "[ ]" means "all of" for array fields.
    private fun anyOf(ids: List<Int>) = ids.joinToString(prefix = "(", postfix = ")")

    @Suppress("LongParameterList")
    fun games(
        searchQuery: String?,
        page: Int,
        pageSize: Int,
        sortOrder: GameSortOrder,
        platformIds: List<Int>?,
        genreIds: List<Int>?,
        nowSeconds: Long
    ): String {
        val isSearch = !searchQuery.isNullOrBlank()
        val offset = (page - 1) * pageSize
        val platforms = platformIds?.takeIf { it.isNotEmpty() } ?: PLAYSTATION_PLATFORM_IDS

        return buildString {
            if (isSearch) {
                val safeQuery = searchQuery.replace("\\", "\\\\").replace("\"", "\\\"")
                append("search \"$safeQuery\"; ")
            }
            append("fields $GAME_FIELDS; ")
            append("where platforms = ${anyOf(platforms)} & cover != null & $EXCLUDED_GAME_TYPES")
            if (!genreIds.isNullOrEmpty()) append(" & genres = ${anyOf(genreIds)}")
            if (!isSearch) {
                // Sort-specific filters keep unrated, undated and unreleased entries out of the top.
                when (sortOrder) {
                    GameSortOrder.RATING_DESC ->
                        append(" & total_rating != null & total_rating_count >= $MIN_RATING_COUNT")
                    GameSortOrder.RELEASE_DATE_DESC ->
                        append(" & first_release_date != null & first_release_date <= $nowSeconds")
                    GameSortOrder.RELEASE_DATE_ASC -> append(" & first_release_date != null")
                    GameSortOrder.NAME_ASC, GameSortOrder.NAME_DESC -> Unit
                }
            }
            append("; ")
            if (!isSearch) {
                val igdbSort = when (sortOrder) {
                    GameSortOrder.RATING_DESC -> "total_rating desc"
                    GameSortOrder.RELEASE_DATE_DESC -> "first_release_date desc"
                    GameSortOrder.RELEASE_DATE_ASC -> "first_release_date asc"
                    GameSortOrder.NAME_ASC -> "name asc"
                    GameSortOrder.NAME_DESC -> "name desc"
                }
                append("sort $igdbSort; ")
            }
            append("limit $pageSize; offset $offset;")
        }
    }

    fun timeToBeat(gameId: Int): String =
        "fields game_id, hastily, normally, completely; where game_id = $gameId; limit 1;"
}
