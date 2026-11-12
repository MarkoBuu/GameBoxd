package com.example.ppp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ppp.R
import com.example.ppp.data.model.LibrarySortOption
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.ui.components.GameCard
import com.example.ppp.ui.components.SortOrFilterDropdown
import com.example.ppp.ui.theme.AccentGold
import com.example.ppp.ui.theme.FavoritesRed
import com.example.ppp.ui.theme.PlayStationLightBlue
import com.example.ppp.ui.theme.TextSecondary
import com.example.ppp.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: GameViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val horizontalScrollState = rememberScrollState()
    val selectedTab by viewModel.selectedLibraryTab.collectAsState()
    val selectedSort by viewModel.selectedLibrarySort.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val showPlatinumsOnly by viewModel.showPlatinumsOnly.collectAsState()
    val selectedYearFilter by viewModel.selectedYearFilter.collectAsState()
    val selectedRatingFilter by viewModel.selectedRatingFilter.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()
    val searchQuery by viewModel.librarySearchQuery.collectAsState()
    val filteredGames by viewModel.filteredLibraryGames.collectAsState()
    val savedGames by viewModel.savedGames.collectAsState()

    var showFilterBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val tabs = listOf(
        PlayStatus.COMPLETED,
        PlayStatus.BACKLOG,
        PlayStatus.DROPPED
    )

    val activeFilterTags = mutableListOf<String>()
    if (showPlatinumsOnly) activeFilterTags.add("🏆")
    if (showFavoritesOnly) activeFilterTags.add("❤️")
    if (selectedGenre != "All") activeFilterTags.add(selectedGenre)
    selectedYearFilter?.let { activeFilterTags.add("$it") }
    selectedRatingFilter?.let { r ->
        val formatted = if (r % 1f == 0f) "${r.toInt()}★" else "$r★"
        activeFilterTags.add(formatted)
    }
    val hasActiveFilters = activeFilterTags.isNotEmpty()

    LaunchedEffect(
        selectedSort,
        searchQuery,
        selectedTab,
        showFavoritesOnly,
        showPlatinumsOnly,
        selectedYearFilter,
        selectedRatingFilter,
        selectedGenre
    ) {
        listState.scrollToItem(0)
    }

    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1 || listState.firstVisibleItemScrollOffset > 200
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Text(
                    text = stringResource(R.string.title_library),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Primary Status Tabs (Completed, Backlog, Dropped)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEach { status ->
                        val isSelected = selectedTab == status
                        val statusCount = savedGames.count { it.playStatus == status }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.setLibraryTab(status)
                            },
                            label = {
                                Text(
                                    text = "${status.displayName} ($statusCount)",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PlayStationLightBlue,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            // Compact Bar: Filter Box + SortDropdown
            item {
                AnimatedVisibility(selectedTab != PlayStatus.BACKLOG) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    showFilterBottomSheet = true
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.filter_by),
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = if (hasActiveFilters) activeFilterTags.joinToString(" · ")
                                    else stringResource(R.string.default_string),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PlayStationLightBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filter_options),
                                    tint = PlayStationLightBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        SortOrFilterDropdown(
                            options = LibrarySortOption.entries,
                            selected = selectedSort,
                            optionLabel = { stringResource(it.displayNameRes) },
                            onSelected = viewModel::setLibrarySort
                        )
                    }
                }
            }

            if (filteredGames.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) {
                                stringResource(R.string.no_games_in_status, selectedTab.displayName)
                            } else {
                                stringResource(
                                    R.string.no_games_matching_in_status,
                                    searchQuery,
                                    selectedTab.displayName
                                )
                            },
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredGames, key = { it.id }) { game ->
                    GameCard(
                        game = game,
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.openRatingDialog(game)
                        },
                        onFavoriteToggle = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.toggleFavorite(game)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = PlayStationLightBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.cd_scroll_to_top)
                )
            }
        }
    }

    // Modal Bottom Sheet for Filter Options
    if (showFilterBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.filter_options),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (hasActiveFilters) {
                        TextButton(
                            onClick = {
                                if (showFavoritesOnly) viewModel.toggleFavoritesFilter()
                                if (showPlatinumsOnly) viewModel.togglePlatinumsFilter()
                                viewModel.setSelectedGenre("All")
                                viewModel.setSelectedYearFilter(null)
                                viewModel.setSelectedRatingFilter(null)
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.reset_filter),
                                color = PlayStationLightBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (selectedTab == PlayStatus.COMPLETED) {
                    // Quick Filters Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.quick_filters),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = showFavoritesOnly,
                                onClick = { viewModel.toggleFavoritesFilter() },
                                label = {
                                    Text(
                                        stringResource(R.string.filter_favorites),
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FavoritesRed,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                            FilterChip(
                                selected = showPlatinumsOnly,
                                onClick = { viewModel.togglePlatinumsFilter() },
                                label = {
                                    Text(
                                        stringResource(R.string.platinum_trophy),
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = MaterialTheme.colorScheme.surface,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    // Rating Filter Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.rating),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedRatingFilter == null,
                                onClick = { viewModel.setSelectedRatingFilter(null) },
                                label = {
                                    Text(
                                        stringResource(R.string.all_ratings),
                                        fontSize = 12.sp
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PlayStationLightBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                            val ratingOptions =
                                listOf(5.0f, 4.5f, 4.0f, 3.5f, 3.0f, 2.5f, 2.0f, 1.5f, 1.0f)
                            ratingOptions.forEach { r ->
                                val isSelected = selectedRatingFilter?.let {
                                    kotlin.math.abs(it - r) < 0.01f
                                } ?: false
                                val labelText = if (r % 1f == 0f) "${r.toInt()}.0 ★" else "$r ★"
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setSelectedRatingFilter(if (isSelected) null else r) },
                                    label = { Text(labelText, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PlayStationLightBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Filter By Genre Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Genre",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.availableGenres.forEach { genre ->
                            val isSelected = selectedGenre == genre
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setSelectedGenre(genre) },
                                label = {
                                    Text(
                                        text = genre,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PlayStationLightBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                // Filter By Year Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (selectedTab == PlayStatus.COMPLETED)
                            stringResource(R.string.completion_year) else
                            stringResource(
                                R.string.year_dropped
                            ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedYearFilter == null,
                            onClick = { viewModel.setSelectedYearFilter(null) },
                            label = {
                                Text(
                                    stringResource(R.string.filter_all_years),
                                    fontSize = 12.sp
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PlayStationLightBlue,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                        availableYears.forEach { yr ->
                            val isYrSelected = selectedYearFilter == yr
                            FilterChip(
                                selected = isYrSelected,
                                onClick = { viewModel.setSelectedYearFilter(yr) },
                                label = { Text("$yr", fontSize = 12.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PlayStationLightBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = { showFilterBottomSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlayStationLightBlue)
                ) {
                    Text(
                        text = stringResource(R.string.apply_filters),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
