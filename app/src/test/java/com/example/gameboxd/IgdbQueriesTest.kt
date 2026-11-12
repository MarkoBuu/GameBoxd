package com.example.gameboxd

import com.example.gameboxd.data.model.GameSortOrder
import com.example.gameboxd.data.remote.IgdbQueries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IgdbQueriesTest {

    private fun query(
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 20,
        sort: GameSortOrder = GameSortOrder.RATING_DESC,
        platformIds: List<Int>? = null,
        genreIds: List<Int>? = null,
        now: Long = 1_700_000_000L
    ) = IgdbQueries.games(search, page, pageSize, sort, platformIds, genreIds, now)

    @Test
    fun topRatedRequiresEnoughRatingsSoSingleVoteEntriesDoNotWin() {
        val q = query(sort = GameSortOrder.RATING_DESC)

        assertTrue(q.contains("total_rating != null"))
        assertTrue(q.contains("total_rating_count >= 10"))
        assertTrue(q.contains("sort total_rating desc;"))
    }

    @Test
    fun newestFirstExcludesUnreleasedAndUndatedGames() {
        val q = query(sort = GameSortOrder.RELEASE_DATE_DESC, now = 1_700_000_000L)

        assertTrue(q.contains("first_release_date != null"))
        assertTrue(q.contains("first_release_date <= 1700000000"))
        assertTrue(q.contains("sort first_release_date desc;"))
    }

    @Test
    fun oldestFirstOnlyRequiresADate() {
        val q = query(sort = GameSortOrder.RELEASE_DATE_ASC)

        assertTrue(q.contains("first_release_date != null"))
        assertFalse(q.contains("first_release_date <="))
        assertTrue(q.contains("sort first_release_date asc;"))
    }

    @Test
    fun nameSortsAddNoExtraFilters() {
        val q = query(sort = GameSortOrder.NAME_ASC)
        // The fields list names these columns, so only the where clause is checked.
        val where = q.substringAfter("where ").substringBefore(";")

        assertFalse(where.contains("total_rating"))
        assertFalse(where.contains("first_release_date"))
        assertTrue(q.contains("sort name asc;"))
    }

    @Test
    fun searchUsesRelevanceOrderingAndNoSortOrSortSpecificFilters() {
        val q = query(search = "zelda", sort = GameSortOrder.RATING_DESC)

        assertTrue(q.startsWith("search \"zelda\";"))
        assertFalse(q.contains("sort "))
        assertFalse(q.contains("total_rating_count"))
    }

    @Test
    fun searchTermsAreEscaped() {
        assertTrue(query(search = "say \"hi\"").startsWith("search \"say \\\"hi\\\"\";"))
        assertTrue(query(search = "back\\slash").startsWith("search \"back\\\\slash\";"))
    }

    @Test
    fun blankSearchIsTreatedAsBrowsing() {
        val q = query(search = "   ")

        assertFalse(q.contains("search "))
        assertTrue(q.contains("sort total_rating desc;"))
    }

    @Test
    fun defaultsCoverEveryPlayStationPlatformAndExcludeDlcAndPacks() {
        val q = query()

        assertTrue(q.contains("platforms = (7, 8, 9, 38, 46, 48, 165, 167, 390)"))
        assertTrue(q.contains("game_type != (1, 5, 13, 14)"))
        assertTrue(q.contains("cover != null"))
        assertFalse(q.contains("genres ="))
    }

    @Test
    fun platformAndGenreFiltersAreApplied() {
        val q = query(platformIds = listOf(167), genreIds = listOf(12, 31))

        assertTrue(q.contains("platforms = (167)"))
        assertTrue(q.contains("genres = (12, 31)"))
    }

    @Test
    fun paginationUsesOffsetAndLimit() {
        assertTrue(query(page = 1, pageSize = 20).endsWith("limit 20; offset 0;"))
        assertTrue(query(page = 3, pageSize = 20).endsWith("limit 20; offset 40;"))
    }

    @Test
    fun timeToBeatQueryTargetsTheGame() {
        assertEquals(
            "fields game_id, hastily, normally, completely; where game_id = 123; limit 1;",
            IgdbQueries.timeToBeat(123)
        )
    }
}
