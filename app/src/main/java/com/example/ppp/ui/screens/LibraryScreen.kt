package com.example.ppp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ppp.R
import com.example.ppp.data.model.LibrarySortOption
import com.example.ppp.data.model.LibraryTab
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.ui.components.GameCard
import com.example.ppp.ui.components.SortDropdown
import com.example.ppp.ui.theme.PlayStationLightBlue
import com.example.ppp.ui.theme.TextSecondary
import com.example.ppp.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    viewModel: GameViewModel,
    listState: LazyListState = rememberLazyListState()
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectedTab by viewModel.selectedLibraryTab.collectAsState()
    val selectedSort by viewModel.selectedLibrarySort.collectAsState()
    val selectedYearFilter by viewModel.selectedYearFilter.collectAsState()
    val searchQuery by viewModel.librarySearchQuery.collectAsState()
    val filteredGames by viewModel.filteredLibraryGames.collectAsState()
    val savedGames by viewModel.savedGames.collectAsState()

    val tabs = LibraryTab.entries
    val yearOptions = listOf<Int?>(null) + viewModel.availableYears
    val horizontalScrollState = rememberScrollState()

    LaunchedEffect(selectedSort, searchQuery, selectedTab, selectedYearFilter) {
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
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.title_library),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.subtitle_library),
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            // Normal Top-Level Tabs: Completed, Backlog, Dropped, Favorites️, Platinums
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val count = when (tab) {
                            LibraryTab.COMPLETED -> savedGames.count { it.playStatus == PlayStatus.COMPLETED }
                            LibraryTab.BACKLOG -> savedGames.count { it.playStatus == PlayStatus.BACKLOG }
                            LibraryTab.DROPPED -> savedGames.count { it.playStatus == PlayStatus.DROPPED }
                            LibraryTab.FAVORITES -> savedGames.count { it.isFavorite }
                            LibraryTab.PLATINUMS -> savedGames.count { it.hasPlatinum }
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.setLibraryTab(tab)
                            },
                            label = {
                                Text(
                                    text = "${tab.displayName} ($count)",
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
            // Compact Row: Year Dropdown + Sort Dropdown
            item {
                AnimatedVisibility(selectedTab != LibraryTab.BACKLOG) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(horizontalScrollState),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 8.dp,
                            alignment = Alignment.End
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SortDropdown(
                            showText = false,
                            options = yearOptions,
                            selected = selectedYearFilter,
                            optionLabel = { yr ->
                                yr?.let { "$it" } ?: stringResource(R.string.filter_all_years)
                            },
                            onSelected = viewModel::setSelectedYearFilter
                        )
                        SortDropdown(
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
                    contentDescription = "Scroll to top"
                )
            }
        }
    }
}
