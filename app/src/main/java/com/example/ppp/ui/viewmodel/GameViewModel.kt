package com.example.ppp.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.GameSortOrder
import com.example.ppp.data.model.LibrarySortOption
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.data.model.ServiceStatus
import com.example.ppp.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed interface ExploreUiState {
    object Loading : ExploreUiState
    data class Success(
        val games: List<Game>,
        val isNextPageLoading: Boolean = false,
        val canLoadMore: Boolean = true
    ) : ExploreUiState

    data class Error(val message: String) : ExploreUiState
}

@Stable
class GameViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _showFavoritesOnly = MutableStateFlow(false)
    val showFavoritesOnly: StateFlow<Boolean> = _showFavoritesOnly.asStateFlow()

    private val _showPlatinumsOnly = MutableStateFlow(false)
    val showPlatinumsOnly: StateFlow<Boolean> = _showPlatinumsOnly.asStateFlow()

    private val _selectedYearFilter = MutableStateFlow<Int?>(null)
    val selectedYearFilter: StateFlow<Int?> = _selectedYearFilter.asStateFlow()

    private val _selectedRatingFilter = MutableStateFlow<Float?>(null)
    val selectedRatingFilter: StateFlow<Float?> = _selectedRatingFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _librarySearchQuery = MutableStateFlow("")
    val librarySearchQuery: StateFlow<String> = _librarySearchQuery.asStateFlow()

    private val _selectedSortOrder = MutableStateFlow(GameSortOrder.RATING_DESC)
    val selectedSortOrder: StateFlow<GameSortOrder> = _selectedSortOrder.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _exploreState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val exploreState: StateFlow<ExploreUiState> = _exploreState.asStateFlow()

    val serviceStatus: StateFlow<ServiceStatus?> = repository.serviceStatusFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _selectedGameForRating = MutableStateFlow<Game?>(null)
    val selectedGameForRating: StateFlow<Game?> = _selectedGameForRating.asStateFlow()

    val savedGames: StateFlow<List<Game>> = repository.savedGamesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedLibraryTab = MutableStateFlow(PlayStatus.COMPLETED)
    val selectedLibraryTab: StateFlow<PlayStatus> = _selectedLibraryTab.asStateFlow()

    private val _selectedLibrarySort = MutableStateFlow(LibrarySortOption.RECENTLY_UPDATED)
    val selectedLibrarySort: StateFlow<LibrarySortOption> = _selectedLibrarySort.asStateFlow()

    val availableGenres = listOf("All", "Action", "RPG", "Adventure", "Shooter", "Indie", "Strategy", "Platformer", "Puzzle", "Sports", "Racing", "Fighting")

    val availableYears: StateFlow<List<Int>> = savedGames.map { list ->
        list.mapNotNull { it.completionYear }.distinct().sortedDescending()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredExploreGames: StateFlow<List<Game>> = exploreState.map { state ->
        if (state !is ExploreUiState.Success) emptyList()
        else state.games
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredLibraryGames: StateFlow<List<Game>> = combine(
        savedGames,
        _selectedLibraryTab,
        _selectedLibrarySort,
        _librarySearchQuery,
        _selectedGenre,
        _showFavoritesOnly,
        _showPlatinumsOnly,
        _selectedYearFilter,
        _selectedRatingFilter
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val savedList = flows[0] as List<Game>
        val tab = flows[1] as PlayStatus
        val sortOption = flows[2] as LibrarySortOption
        val query = flows[3] as String
        val genre = flows[4] as String
        val favOnly = flows[5] as Boolean
        val platOnly = flows[6] as Boolean
        val yearFilter = flows[7] as Int?
        val ratingFilter = flows[8] as Float?

        val trimmedQuery = query.trim().lowercase()
        val queryTokens = if (trimmedQuery.isNotEmpty()) {
            trimmedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        } else {
            emptyList()
        }

        val filteredByStatus = savedList.filter { it.playStatus == tab }

        val filtered = filteredByStatus
            .asSequence()
            .filter { !favOnly || it.isFavorite }
            .filter { !platOnly || it.hasPlatinum }
            .filter { yearFilter == null || it.completionYear == yearFilter }
            .filter { ratingFilter == null || kotlin.math.abs(it.userRating - ratingFilter) < 0.01f }
            .filter { genre == "All" || it.genres.any { g -> g.equals(genre, ignoreCase = true) } }
            .filter {
                if (queryTokens.isEmpty()) true
                else {
                    val titleLower = it.title.lowercase()
                    queryTokens.all { token -> repository.matchesToken(titleLower, token) }
                }
            }
            .toList()
        when (sortOption) {
            LibrarySortOption.RECENTLY_UPDATED -> filtered.sortedByDescending { it.updatedAt }
            LibrarySortOption.HIGHEST_RATING -> filtered.sortedByDescending { it.userRating }
            LibrarySortOption.RELEASE_DATE -> filtered.sortedByDescending { it.releaseDate ?: "" }
            LibrarySortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
            LibrarySortOption.MOST_PLAYED -> filtered.sortedByDescending { it.playtimeHours }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var currentPage = 1
    private var currentGamesList = mutableListOf<Game>()
    private var canLoadMorePages = true
    private var isPageLoading = false
    private var searchDebounceJob: Job? = null

    init {
        fetchInitialPage()
        if (REMOTE_STATUS_URL.isNotBlank()) {
            checkRemoteStatus(REMOTE_STATUS_URL)
        }
        viewModelScope.launch {
            savedGames.collect { saved -> remergeExploreGames(saved) }
        }
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun checkRemoteStatus(url: String) {
        viewModelScope.launch {
            repository.checkRemoteStatus(url)
        }
    }

    fun dismissServiceStatus() {
        repository.dismissServiceStatus()
    }

    fun toggleFavoritesFilter() {
        _showFavoritesOnly.value = !_showFavoritesOnly.value
    }

    fun togglePlatinumsFilter() {
        _showPlatinumsOnly.value = !_showPlatinumsOnly.value
    }

    fun setSelectedYearFilter(year: Int?) {
        _selectedYearFilter.value = year
    }

    fun setSelectedRatingFilter(rating: Float?) {
        _selectedRatingFilter.value = rating
    }

    fun toggleFavorite(game: Game) {
        // Only COMPLETED games can be added to favorites
        if (game.playStatus == PlayStatus.COMPLETED) {
            repository.toggleFavorite(game)
        }
    }

    fun setSelectedGenre(genre: String) {
        _selectedGenre.value = genre
    }

    private fun remergeExploreGames(saved: List<Game>) {
        val state = _exploreState.value
        if (state !is ExploreUiState.Success) return

        val savedMap = saved.associateBy { it.id }
        _exploreState.value = state.copy(
            games = currentGamesList
                .map { game ->
                    val existingSaved = savedMap[game.id]
                    if (existingSaved != null) {
                        if (existingSaved.genres.isEmpty() && game.genres.isNotEmpty()) {
                            val enriched = existingSaved.copy(genres = game.genres)
                            repository.saveGameRating(
                                game = enriched,
                                userRating = enriched.userRating,
                                status = enriched.playStatus,
                                reviewText = enriched.reviewText,
                                hasPlatinum = enriched.hasPlatinum,
                                playtimeHours = enriched.playtimeHours,
                                isFavorite = enriched.isFavorite,
                                completionYear = enriched.completionYear
                            )
                            enriched
                        } else {
                            existingSaved
                        }
                    } else {
                        game
                    }
                }
                .distinctBy { it.id }
        )
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(400.milliseconds) // Debounce search input
            fetchInitialPage()
        }
    }

    fun onSortOrderChanged(order: GameSortOrder) {
        if (_selectedSortOrder.value != order) {
            _selectedSortOrder.value = order
            fetchInitialPage()
        }
    }

    fun setLibraryTab(status: PlayStatus) {
        _selectedLibraryTab.value = status
    }

    fun setLibrarySort(sortOption: LibrarySortOption) {
        _selectedLibrarySort.value = sortOption
    }

    fun onLibrarySearchChanged(query: String) {
        _librarySearchQuery.value = query
    }

    fun openRatingDialog(game: Game) {
        val existing = repository.getGameById(game.id)
        _selectedGameForRating.value = existing ?: game
    }

    fun closeRatingDialog() {
        _selectedGameForRating.value = null
    }

    fun saveRating(
        game: Game,
        rating: Float,
        status: PlayStatus,
        review: String,
        hasPlatinum: Boolean,
        playtimeHours: Int,
        isFavorite: Boolean = game.isFavorite,
        completionYear: Int? = game.completionYear
    ) {
        val isCompleted = status == PlayStatus.COMPLETED
        val isBacklog = status == PlayStatus.BACKLOG

        // Only COMPLETED games allow ratings, reviews, and favorites
        val effectiveRating = if (isCompleted) rating else 0f
        val effectivePlatinum = hasPlatinum && isCompleted
        val effectiveReview = if (isCompleted) review else ""
        val effectiveFavorite = isFavorite && isCompleted
        val effectiveHours = if (isBacklog) 0 else playtimeHours
        val effectiveYear = if (isBacklog) null else completionYear

        repository.saveGameRating(
            game = game,
            userRating = effectiveRating,
            status = status,
            reviewText = effectiveReview,
            hasPlatinum = effectivePlatinum,
            playtimeHours = effectiveHours,
            isFavorite = effectiveFavorite,
            completionYear = effectiveYear
        )
        closeRatingDialog()
    }

    fun removeGame(gameId: Int) {
        repository.removeGameFromLibrary(gameId)
        closeRatingDialog()
    }

    fun exportDataJson(): String {
        return repository.exportLibraryJson()
    }

    fun importDataJson(jsonString: String): Boolean {
        return repository.importLibraryJson(jsonString)
    }

    fun loadNextPage() {
        if (isPageLoading || !canLoadMorePages) return

        viewModelScope.launch {
            isPageLoading = true
            val currentState = _exploreState.value
            if (currentState is ExploreUiState.Success) {
                _exploreState.value = currentState.copy(isNextPageLoading = true)
            }

            try {
                val nextPage = currentPage + 1
                val newGames = repository.fetchPlayStationGames(
                    query = _searchQuery.value,
                    page = nextPage,
                    sortOrder = _selectedSortOrder.value
                )

                if (newGames.isEmpty()) {
                    canLoadMorePages = false
                } else {
                    currentPage = nextPage
                    val uniqueNewGames =
                        newGames.filter { newGame -> currentGamesList.none { it.id == newGame.id } }
                    currentGamesList.addAll(uniqueNewGames)
                }

                val savedMap = savedGames.value.associateBy { it.id }
                val merged = currentGamesList.map { savedMap[it.id] ?: it }.distinctBy { it.id }

                _exploreState.value = ExploreUiState.Success(
                    games = merged,
                    isNextPageLoading = false,
                    canLoadMore = canLoadMorePages
                )
            } catch (_: Exception) {
                val latest = _exploreState.value
                if (latest is ExploreUiState.Success) {
                    _exploreState.value = latest.copy(isNextPageLoading = false)
                }
            } finally {
                isPageLoading = false
            }
        }
    }

    private var fetchJob: Job? = null

    private fun fetchInitialPage() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            isPageLoading = true
            currentPage = 1
            canLoadMorePages = true
            currentGamesList.clear()
            _exploreState.value = ExploreUiState.Loading

            try {
                val games = repository.fetchPlayStationGames(
                    query = _searchQuery.value,
                    page = 1,
                    sortOrder = _selectedSortOrder.value
                )
                val uniqueGames = games.distinctBy { it.id }
                currentGamesList.addAll(uniqueGames)
                canLoadMorePages = games.size >= 20

                val savedMap = savedGames.value.associateBy { it.id }
                val merged = currentGamesList.map { savedMap[it.id] ?: it }.distinctBy { it.id }

                _exploreState.value = ExploreUiState.Success(
                    games = merged,
                    isNextPageLoading = false,
                    canLoadMore = canLoadMorePages
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _exploreState.value = ExploreUiState.Error(e.message ?: "Failed to fetch games")
            } finally {
                isPageLoading = false
            }
        }
    }

    companion object {
        // Replace this URL with your raw GitHub Gist URL
        private const val REMOTE_STATUS_URL = ""
    }
}
