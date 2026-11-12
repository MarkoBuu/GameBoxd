package com.example.ppp

import com.example.ppp.data.local.GameEntity
import com.example.ppp.data.model.ExploreGenre
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.GameSortOrder
import com.example.ppp.data.model.PlatformFilter
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.data.repository.GameRepository
import com.example.ppp.data.repository.ImportResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class GameRepositoryTest {

    // region library persistence

    @Test
    fun untrackedGameThatIsNotFavoritedIsNotKept() = runTest {
        val game = savedGame(1, "Astro Bot")
        val dao = FakeGameDao(listOf(game))
        val repo = repository(dao = dao)

        repo.saveGameRating(game, userRating = 0f, status = PlayStatus.NONE, reviewText = "", isFavorite = false)

        assertTrue(dao.games.isEmpty())
    }

    @Test
    fun savingACompletedGameStoresItWithAFreshUpdateTime() = runTest {
        val dao = FakeGameDao()
        val repo = repository(dao = dao)

        repo.saveGameRating(
            savedGame(1, "Astro Bot", status = PlayStatus.NONE, updatedAt = 1L),
            userRating = 5f,
            status = PlayStatus.COMPLETED,
            reviewText = "great"
        )

        val saved = dao.games.single()
        assertEquals(5f, saved.userRating)
        assertEquals(PlayStatus.COMPLETED, saved.playStatus)
        assertTrue(saved.updatedAt > 1L)
    }

    // endregion

    // region fetching

    @Test
    fun savedEntryIsReusedWhenIdAndTitleMatch() = runTest {
        val dao = FakeGameDao(listOf(savedGame(5, "Alpha", rating = 3.5f)))
        val repo = repository(FakeIgdbApi(listOf(dto(5, "Alpha"))), dao)

        val game = repo.fetchPlayStationGames().single()

        assertEquals(3.5f, game.userRating)
        assertEquals(PlayStatus.COMPLETED, game.playStatus)
    }

    @Test
    fun unmigratedRawgIdCollidingWithADifferentGameIsNotReused() = runTest {
        // Saved "Alpha" still carries a RAWG id (5) that IGDB uses for the unrelated "Beta".
        val dao = FakeGameDao(listOf(savedGame(5, "Alpha", rating = 3.5f)))
        val repo = repository(FakeIgdbApi(listOf(dto(5, "Beta"))), dao)

        val game = repo.fetchPlayStationGames().single()

        assertEquals("Beta", game.title)
        assertEquals(PlayStatus.NONE, game.playStatus)
        assertEquals(0f, game.userRating)
    }

    @Test
    fun onlyPlayStationPlatformsAreShown() = runTest {
        val api = FakeIgdbApi(
            listOf(dto(1, "Multi", platforms = listOf("PC (Microsoft Windows)", "PlayStation 5", "Xbox One")))
        )

        val game = repository(api).fetchPlayStationGames().single()

        assertEquals(listOf("PlayStation 5"), game.platforms)
    }

    @Test
    fun igdbRatingIsConvertedToAFiveStarScale() = runTest {
        val api = FakeIgdbApi(listOf(dto(1, "Game", rating = 87.0)))

        assertEquals(4.4, repository(api).fetchPlayStationGames().single().igdbRating, 0.0001)
    }

    @Test
    fun platformAndGenreFiltersReachTheApi() = runTest {
        val api = FakeIgdbApi(listOf(dto(1, "Game")))

        repository(api).fetchPlayStationGames(
            platform = PlatformFilter.PS5,
            genre = ExploreGenre.RPG
        )

        assertEquals(listOf(167), api.lastPlatformIds)
        assertEquals(listOf(12), api.lastGenreIds)
    }

    @Test
    fun missingCredentialsFailFastWithoutAnyRequest() = runTest {
        val api = FakeIgdbApi(listOf(dto(1, "Game")))
        val repo = repository(api, clientSecret = "")

        try {
            repo.fetchPlayStationGames()
            fail("expected an exception")
        } catch (_: IllegalStateException) {
            // expected
        }
        assertEquals(0, api.requestCount)
        assertNotNull(repo.serviceStatusFlow.value)
    }

    // endregion

    // region offline cache

    @Test
    fun failedFirstPageFallsBackToTheLastGoodResults() = runTest {
        val api = FakeIgdbApi(listOf(dto(1, "Cached One"), dto(2, "Cached Two")))
        val repo = repository(api)
        repo.fetchPlayStationGames() // warms the cache

        api.failure = IOException("offline")
        val offline = repo.fetchPlayStationGames()

        assertEquals(listOf("Cached One", "Cached Two"), offline.map { it.title })
        assertTrue(repo.serviceStatusFlow.value?.isOutage == true)
    }

    @Test
    fun cachedFallbackStillReflectsTheUsersSavedData() = runTest {
        val dao = FakeGameDao()
        val api = FakeIgdbApi(listOf(dto(1, "Game")))
        val repo = repository(api, dao)
        repo.fetchPlayStationGames()
        dao.insertOrUpdate(GameEntity.fromGame(savedGame(1, "Game", rating = 2f)))

        api.failure = IOException("offline")

        assertEquals(2f, repo.fetchPlayStationGames().single().userRating)
    }

    @Test
    fun failureWithoutACacheIsReportedToTheCaller() = runTest {
        val api = FakeIgdbApi().apply { failure = IOException("offline") }

        try {
            repository(api).fetchPlayStationGames()
            fail("expected an exception")
        } catch (_: IOException) {
            // expected
        }
    }

    @Test
    fun laterPagesSearchesAndOtherSortsNeverUseTheCache() = runTest {
        val api = FakeIgdbApi(List(30) { dto(it + 1, "Game ${it + 1}") })
        val repo = repository(api)
        repo.fetchPlayStationGames() // cache only exists for the default first page
        api.failure = IOException("offline")

        for (attempt in listOf<suspend () -> Unit>(
            { repo.fetchPlayStationGames(page = 2) },
            { repo.fetchPlayStationGames(query = "game") },
            { repo.fetchPlayStationGames(sortOrder = GameSortOrder.NAME_ASC) }
        )) {
            try {
                attempt()
                fail("expected an exception")
            } catch (_: IOException) {
                // expected
            }
        }
    }

    // endregion

    // region backup import

    @Test
    fun importKeepsNewerLocalEntriesAndAddsOrUpdatesTheRest() = runTest {
        val dao = FakeGameDao(
            listOf(
                savedGame(1, "Local newer", rating = 3f, updatedAt = 5_000L),
                savedGame(2, "Local older", rating = 3f, updatedAt = 1_000L)
            )
        )
        val repo = repository(dao = dao)
        val backup = exportOf(
            savedGame(1, "Local newer", rating = 1f, updatedAt = 2_000L),
            savedGame(2, "Local older", rating = 5f, updatedAt = 3_000L),
            savedGame(3, "Brand new", rating = 4f, updatedAt = 1_000L)
        )

        val result = repo.importLibraryJson(backup)

        assertEquals(ImportResult(added = 1, updated = 1, skipped = 1), result)
        val byId = dao.games.associateBy { it.id }
        assertEquals(3f, byId.getValue(1).userRating)
        assertEquals(5f, byId.getValue(2).userRating)
        assertEquals(4f, byId.getValue(3).userRating)
    }

    @Test
    fun backupsFromTheRawgEraStillImport() = runTest {
        val dao = FakeGameDao()
        val legacyJson = """[{"id":42,"title":"Old Game","rawgRating":4.2,"playStatus":"COMPLETED","userRating":4.0}]"""

        val result = repository(dao = dao).importLibraryJson(legacyJson)

        assertEquals(1, result?.added)
        assertEquals(4.2, dao.games.single().igdbRating, 0.0001)
    }

    @Test
    fun invalidOrEmptyBackupsAreRejectedWithoutTouchingTheLibrary() = runTest {
        val dao = FakeGameDao(listOf(savedGame(1, "Keep me")))
        val repo = repository(dao = dao)

        assertNull(repo.importLibraryJson("not json"))
        assertNull(repo.importLibraryJson("[]"))
        assertEquals(1, dao.games.size)
    }

    @Test
    fun importReopensTheLegacyIdRematch() = runTest {
        val store = InMemoryStore().apply { putBoolean(GameRepository.KEY_IGDB_MIGRATION_DONE, true) }
        val repo = repository(store = store)

        repo.importLibraryJson(exportOf(savedGame(1, "Game")))

        assertFalse(store.getBoolean(GameRepository.KEY_IGDB_MIGRATION_DONE))
    }

    @Test
    fun exportThenImportRoundTrips() = runTest {
        val original = FakeGameDao(listOf(savedGame(1, "A", favorite = true), savedGame(2, "B")))
        val exported = repository(dao = original).exportLibraryJson()

        val target = FakeGameDao()
        repository(dao = target).importLibraryJson(exported)

        assertEquals(original.games, target.games)
    }

    // endregion

    // region relink

    @Test
    fun relinkMovesTheGameToTheNewIdKeepingUserData() = runTest {
        val dao = FakeGameDao(listOf(savedGame(1, "Wrong Game", rating = 4.5f)))
        val repo = repository(dao = dao)
        val target = repository(FakeIgdbApi(listOf(dto(99, "Right Game")))).fetchPlayStationGames().single()

        val success = repo.relinkGame(dao.games.single(), target)

        assertTrue(success)
        val relinked = dao.games.single()
        assertEquals(99, relinked.id)
        assertEquals("Right Game", relinked.title)
        assertEquals(4.5f, relinked.userRating)
        assertEquals("review of Wrong Game", relinked.reviewText)
    }

    @Test
    fun relinkRefusesToMergeIntoAnotherSavedGame() = runTest {
        val dao = FakeGameDao(listOf(savedGame(1, "One"), savedGame(2, "Two")))
        val repo = repository(dao = dao)
        val target = savedGame(2, "Two")

        assertFalse(repo.relinkGame(savedGame(1, "One"), target))
        assertEquals(listOf(1, 2), dao.games.map { it.id })
    }

    // endregion

    // region average playtime

    @Test
    fun averagePlaytimeIsRoundedToWholeHoursWithAMinimumOfOne() = runTest {
        val api = FakeIgdbApi()
        val repo = repository(api)

        api.timeToBeatSeconds = 36_000
        assertEquals(10, repo.fetchAveragePlaytimeHours(1))
        api.timeToBeatSeconds = 5_400
        assertEquals(2, repo.fetchAveragePlaytimeHours(1))
        api.timeToBeatSeconds = 600
        assertEquals(1, repo.fetchAveragePlaytimeHours(1))
    }

    @Test
    fun averagePlaytimeIsZeroWhenUnknownOrFailing() = runTest {
        val api = FakeIgdbApi()
        val repo = repository(api)

        api.timeToBeatSeconds = null
        assertEquals(0, repo.fetchAveragePlaytimeHours(1))

        api.failure = IOException("offline")
        assertEquals(0, repo.fetchAveragePlaytimeHours(1))
    }

    @Test
    fun cachingAveragePlaytimeDoesNotChangeTheUpdateTime() = runTest {
        val dao = FakeGameDao(listOf(savedGame(1, "Game", updatedAt = 1_234L)))

        repository(dao = dao).cacheAveragePlaytime(1, 12)

        val game = dao.games.single()
        assertEquals(12, game.averagePlaytime)
        assertEquals(1_234L, game.updatedAt)
    }

    // endregion

    private fun exportOf(vararg games: Game): String =
        Json.encodeToString(ListSerializer(Game.serializer()), games.toList())
}
