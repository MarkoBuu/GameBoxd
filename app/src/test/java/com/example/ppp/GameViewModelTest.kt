package com.example.ppp

import com.example.ppp.data.model.LibrarySortOption
import com.example.ppp.data.model.PlatformFilter
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.ui.viewmodel.ExploreUiState
import com.example.ppp.ui.viewmodel.GameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        dao: FakeGameDao = FakeGameDao(),
        api: FakeIgdbApi = FakeIgdbApi(listOf(dto(1, "Catalog Game")))
    ) = GameViewModel(repository(api, dao))

    // region library

    @Test
    fun libraryShowsOnlyTheSelectedTab() = runTest(dispatcher) {
        val vm = viewModel(FakeGameDao(listOf(savedGame(1, "Done"), savedGame(2, "Later", PlayStatus.BACKLOG))))
        backgroundScope.launch { vm.filteredLibraryGames.collect {} }

        assertEquals(listOf("Done"), vm.filteredLibraryGames.value.map { it.title })

        vm.setLibraryTab(PlayStatus.BACKLOG)
        assertEquals(listOf("Later"), vm.filteredLibraryGames.value.map { it.title })
    }

    @Test
    fun favoritesFilterNarrowsTheLibrary() = runTest(dispatcher) {
        val vm = viewModel(FakeGameDao(listOf(savedGame(1, "Plain"), savedGame(2, "Loved", favorite = true))))
        backgroundScope.launch { vm.filteredLibraryGames.collect {} }

        vm.toggleFavoritesFilter()

        assertEquals(listOf("Loved"), vm.filteredLibraryGames.value.map { it.title })
    }

    @Test
    fun librarySearchTreatsArabicAndRomanNumeralsAsEqual() = runTest(dispatcher) {
        val vm = viewModel(
            FakeGameDao(listOf(savedGame(1, "Final Fantasy VII"), savedGame(2, "Final Fantasy X")))
        )
        backgroundScope.launch { vm.filteredLibraryGames.collect {} }

        vm.onLibrarySearchChanged("final fantasy 7")

        assertEquals(listOf("Final Fantasy VII"), vm.filteredLibraryGames.value.map { it.title })
    }

    @Test
    fun librarySortsByHighestRating() = runTest(dispatcher) {
        val vm = viewModel(
            FakeGameDao(listOf(savedGame(1, "Low", rating = 2f), savedGame(2, "High", rating = 5f), savedGame(3, "Mid", rating = 3.5f)))
        )
        backgroundScope.launch { vm.filteredLibraryGames.collect {} }

        vm.setLibrarySort(LibrarySortOption.HIGHEST_RATING)

        assertEquals(listOf("High", "Mid", "Low"), vm.filteredLibraryGames.value.map { it.title })
    }

    // endregion

    // region saving

    @Test
    fun backlogEntriesNeverKeepRatingsReviewsOrPlaytime() = runTest(dispatcher) {
        val dao = FakeGameDao()
        val vm = viewModel(dao)

        vm.saveRating(
            game = savedGame(1, "Game", status = PlayStatus.NONE),
            rating = 5f,
            status = PlayStatus.BACKLOG,
            review = "should be dropped",
            hasPlatinum = true,
            playtimeHours = 40,
            isFavorite = true,
            completionYear = 2024
        )

        val saved = dao.games.single()
        assertEquals(PlayStatus.BACKLOG, saved.playStatus)
        assertEquals(0f, saved.userRating)
        assertEquals("", saved.reviewText)
        assertFalse(saved.hasPlatinum)
        assertFalse(saved.isFavorite)
        assertEquals(0, saved.playtimeHours)
        assertNull(saved.completionYear)
    }

    @Test
    fun onlyCompletedGamesCanBeFavorited() = runTest(dispatcher) {
        val dao = FakeGameDao(listOf(savedGame(1, "Later", status = PlayStatus.BACKLOG)))
        val vm = viewModel(dao)

        vm.toggleFavorite(dao.games.single())

        assertFalse(dao.games.single().isFavorite)
    }

    @Test
    fun removingAGameClosesTheDialogAndDeletesIt() = runTest(dispatcher) {
        val dao = FakeGameDao(listOf(savedGame(1, "Game")))
        val vm = viewModel(dao)
        vm.openRatingDialog(dao.games.single())

        vm.removeGame(1)

        assertTrue(dao.games.isEmpty())
        assertNull(vm.selectedGameForRating.value)
    }

    // endregion

    // region dialog extras

    @Test
    fun openingTheDialogLoadsAveragePlaytimeInTheBackground() = runTest(dispatcher) {
        val api = FakeIgdbApi(listOf(dto(1, "Catalog Game"))).apply { timeToBeatSeconds = 36_000 }
        val vm = viewModel(api = api)

        vm.openRatingDialog(savedGame(1, "Catalog Game", status = PlayStatus.NONE))

        assertEquals(10, vm.selectedGameForRating.value?.averagePlaytime)
    }

    @Test
    fun averagePlaytimeIsAlsoStoredOnAlreadySavedGames() = runTest(dispatcher) {
        val dao = FakeGameDao(listOf(savedGame(1, "Game", updatedAt = 777L)))
        val api = FakeIgdbApi().apply { timeToBeatSeconds = 7_200 }
        val vm = viewModel(dao, api)

        vm.openRatingDialog(dao.games.single())

        assertEquals(2, dao.games.single().averagePlaytime)
        assertEquals(777L, dao.games.single().updatedAt)
    }

    // endregion

    // region explore

    @Test
    fun exploreLoadsTheFirstPageOnStart() = runTest(dispatcher) {
        val vm = viewModel()

        val state = vm.exploreState.value
        assertTrue(state is ExploreUiState.Success)
        assertEquals(listOf("Catalog Game"), (state as ExploreUiState.Success).games.map { it.title })
    }

    @Test
    fun exploreShowsAnErrorWhenTheFirstLoadFailsWithNoCache() = runTest(dispatcher) {
        val api = FakeIgdbApi().apply { failure = IOException("offline") }

        val vm = viewModel(api = api)

        assertTrue(vm.exploreState.value is ExploreUiState.Error)
    }

    @Test
    fun changingThePlatformChipRefetchesWithThatPlatform() = runTest(dispatcher) {
        val api = FakeIgdbApi(listOf(dto(1, "Catalog Game")))
        val vm = viewModel(api = api)

        vm.onPlatformFilterChanged(PlatformFilter.PS5)

        assertEquals(listOf(167), api.lastPlatformIds)
        assertEquals(PlatformFilter.PS5, vm.explorePlatform.value)
    }

    @Test
    fun retryAfterAFailureRecovers() = runTest(dispatcher) {
        val api = FakeIgdbApi(listOf(dto(1, "Catalog Game"))).apply { failure = IOException("offline") }
        val vm = viewModel(api = api)
        assertTrue(vm.exploreState.value is ExploreUiState.Error)

        api.failure = null
        vm.retryFetch()

        assertTrue(vm.exploreState.value is ExploreUiState.Success)
    }

    // endregion
}
