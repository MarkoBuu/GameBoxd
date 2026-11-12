package com.example.ppp

import com.example.ppp.data.model.PlayStatus
import com.example.ppp.data.repository.GameRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LegacyIdMigrationTest {

    @Test
    fun remapsExactTitleMatchAndKeepsAllUserData() = runTest {
        val old = savedGame(100, "God of War", rating = 4.5f, favorite = true)
        val dao = FakeGameDao(listOf(old))
        val store = InMemoryStore()
        val repo = repository(FakeIgdbApi(listOf(dto(2000, "God of War"))), dao, store)

        repo.migrateLegacyRawgIds()

        val migrated = dao.games.single()
        assertEquals(2000, migrated.id)
        assertEquals(4.5f, migrated.userRating)
        assertEquals("review of God of War", migrated.reviewText)
        assertEquals(PlayStatus.COMPLETED, migrated.playStatus)
        assertTrue(migrated.isFavorite)
        assertTrue(migrated.hasPlatinum)
        assertEquals(30, migrated.playtimeHours)
        assertEquals(2022, migrated.completionYear)
        assertTrue(store.getBoolean(GameRepository.KEY_IGDB_MIGRATION_DONE))
    }

    @Test
    fun titleComparisonIgnoresCasePunctuationAndAccents() = runTest {
        val dao = FakeGameDao(listOf(savedGame(1, "pokemon: legends arceus")))
        val repo = repository(FakeIgdbApi(listOf(dto(50, "Pokémon Legends: Arceus"))), dao)

        repo.migrateLegacyRawgIds()

        assertEquals(50, dao.games.single().id)
    }

    @Test
    fun unmatchedGameKeepsItsIdAndData() = runTest {
        val dao = FakeGameDao(listOf(savedGame(100, "Obscure Title")))
        val store = InMemoryStore()
        val repo = repository(FakeIgdbApi(listOf(dto(2000, "Something Else"))), dao, store)

        repo.migrateLegacyRawgIds()

        assertEquals(100, dao.games.single().id)
        // Nothing failed, so the migration is considered finished even though one game didn't match.
        assertTrue(store.getBoolean(GameRepository.KEY_IGDB_MIGRATION_DONE))
    }

    @Test
    fun doesNotOverwriteAnotherLibraryEntryThatHoldsTheTargetId() = runTest {
        // "Alpha" would map to IGDB id 7, but "Beta" already lives at id 7.
        val dao = FakeGameDao(listOf(savedGame(5, "Alpha"), savedGame(7, "Beta")))
        val repo = repository(FakeIgdbApi(listOf(dto(7, "Alpha"), dto(8, "Beta"))), dao)

        repo.migrateLegacyRawgIds()

        val byTitle = dao.games.associateBy { it.title }
        assertEquals(2, dao.games.size)
        assertEquals(5, byTitle.getValue("Alpha").id)
        assertEquals(8, byTitle.getValue("Beta").id)
    }

    @Test
    fun runningTwiceChangesNothing() = runTest {
        val dao = FakeGameDao(listOf(savedGame(100, "God of War")))
        val store = InMemoryStore()
        val api = FakeIgdbApi(listOf(dto(2000, "God of War")))
        val repo = repository(api, dao, store)

        repo.migrateLegacyRawgIds()
        val afterFirst = dao.games
        val requestsAfterFirst = api.requestCount
        repo.migrateLegacyRawgIds()

        assertEquals(afterFirst, dao.games)
        assertEquals("done flag skips the second run entirely", requestsAfterFirst, api.requestCount)
    }

    @Test
    fun entryAlreadyOnIgdbIdMapsToItself() = runTest {
        val dao = FakeGameDao(listOf(savedGame(2000, "God of War")))
        val repo = repository(FakeIgdbApi(listOf(dto(2000, "God of War"))), dao)

        repo.migrateLegacyRawgIds()

        assertEquals(2000, dao.games.single().id)
    }

    @Test
    fun networkFailureLeavesLibraryUntouchedAndAllowsRetry() = runTest {
        val dao = FakeGameDao(listOf(savedGame(100, "God of War")))
        val store = InMemoryStore()
        val api = FakeIgdbApi(listOf(dto(2000, "God of War"))).apply { failure = IOException("offline") }
        val repo = repository(api, dao, store)

        repo.migrateLegacyRawgIds()

        assertEquals(100, dao.games.single().id)
        assertFalse(store.getBoolean(GameRepository.KEY_IGDB_MIGRATION_DONE))

        api.failure = null
        repo.migrateLegacyRawgIds()
        assertEquals(2000, dao.games.single().id)
        assertTrue(store.getBoolean(GameRepository.KEY_IGDB_MIGRATION_DONE))
    }

    @Test
    fun emptyLibraryIsMarkedDoneWithoutAnyRequest() = runTest {
        val store = InMemoryStore()
        val api = FakeIgdbApi()

        repository(api, store = store).migrateLegacyRawgIds()

        assertEquals(0, api.requestCount)
        assertTrue(store.getBoolean(GameRepository.KEY_IGDB_MIGRATION_DONE))
    }

    @Test
    fun missingCredentialsMakeNoRequestsAndDoNotMarkDone() = runTest {
        val store = InMemoryStore()
        val api = FakeIgdbApi(listOf(dto(2000, "God of War")))
        val repo = repository(api, FakeGameDao(listOf(savedGame(100, "God of War"))), store, clientId = "")

        repo.migrateLegacyRawgIds()

        assertEquals(0, api.requestCount)
        assertFalse(store.getBoolean(GameRepository.KEY_IGDB_MIGRATION_DONE))
    }
}
