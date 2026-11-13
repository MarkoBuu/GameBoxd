package com.example.gameboxd.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.example.gameboxd.R
import com.example.gameboxd.ui.components.ServerStatusBanner
import com.example.gameboxd.ui.screens.ExploreScreen
import com.example.gameboxd.ui.screens.GameDetailDialog
import com.example.gameboxd.ui.screens.LibraryScreen
import com.example.gameboxd.ui.screens.StatsScreen
import com.example.gameboxd.ui.theme.PlayStationLightBlue
import com.example.gameboxd.ui.theme.TextSecondary
import com.example.gameboxd.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    viewModel: GameViewModel
) {
    val navigationState = rememberNavigationState(
        startRoute = MainScreens.Explore,
        topLevelRoutes = setOf(MainScreens.Explore, MainScreens.Library, MainScreens.Stats)
    )
    val navigator = remember { Navigator(navigationState) }
    val currentRoute = navigationState.topLevelRoute

    val selectedGameForRating by viewModel.selectedGameForRating.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val serviceStatus by viewModel.serviceStatus.collectAsState()
    val relinkResults by viewModel.relinkResults.collectAsState()
    val relinkLoading by viewModel.relinkLoading.collectAsState()

    val exploreListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val statsListState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        TabInfo(MainScreens.Explore, "Explore", Icons.Default.Explore),
        TabInfo(MainScreens.Library, "Library", Icons.Default.VideogameAsset),
        TabInfo(MainScreens.Stats, "Stats", Icons.Default.BarChart)
    )

    val barColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
    val isSearchable = currentRoute == MainScreens.Explore || currentRoute == MainScreens.Library
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

    val exploreQuery by viewModel.searchQuery.collectAsState()
    val libraryQuery by viewModel.librarySearchQuery.collectAsState()

    fun closeSearch() {
        isSearchActive = false
        when (currentRoute) {
            MainScreens.Explore -> viewModel.onSearchQueryChanged("")
            MainScreens.Library -> viewModel.onLibrarySearchChanged("")
        }
    }

    var lastRoute by remember { mutableStateOf(currentRoute) }
    LaunchedEffect(currentRoute) {
        if (lastRoute != currentRoute) {
            isSearchActive = false
            when (lastRoute) {
                MainScreens.Explore -> {
                    viewModel.onSearchQueryChanged("")
                    viewModel.dismissServiceStatus()
                }
                MainScreens.Library -> viewModel.onLibrarySearchChanged("")
            }
            lastRoute = currentRoute
        }
    }

    BackHandler(enabled = isSearchActive) { closeSearch() }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = barColor,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = ::closeSearch) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_close_search)
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_controller_logo),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
                title = {
                    if (isSearchActive) {
                        if (currentRoute == MainScreens.Explore) {
                            TopBarSearchField(
                                query = exploreQuery,
                                placeholder = stringResource(R.string.search_placeholder_explore),
                                onQueryChange = viewModel::onSearchQueryChanged,
                                onClose = ::closeSearch
                            )
                        } else if (currentRoute == MainScreens.Library) {
                            TopBarSearchField(
                                query = libraryQuery,
                                placeholder = stringResource(R.string.search_placeholder_library),
                                onQueryChange = viewModel::onLibrarySearchChanged,
                                onClose = ::closeSearch
                            )
                        }
                    } else {
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        if (isSearchable) {
                            IconButton(onClick = {
                                isSearchActive = true
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.cd_search),
                                    tint = if (isDarkTheme) Color.White else Color.Black
                                )
                            }
                        }
                        IconButton(onClick = {
                            viewModel.toggleTheme()
                        }) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = stringResource(R.string.toggle_theme),
                                tint = if (isDarkTheme) Color.White else Color.Black
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = barColor,
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navigator.navigate(tab.route)
                            scope.launch {
                                when (tab.route) {
                                    MainScreens.Explore -> exploreListState.scrollToItem(0)
                                    MainScreens.Library -> libraryListState.scrollToItem(0)
                                    MainScreens.Stats -> statsListState.scrollTo(0)
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) PlayStationLightBlue else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (isSelected) PlayStationLightBlue else TextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            serviceStatus?.let { status ->
                ServerStatusBanner(
                    status = status,
                    onDismiss = { viewModel.dismissServiceStatus() }
                )
            }
            val entries = navigationState.toEntries { key ->
                when (key) {
                    MainScreens.Explore -> NavEntry(key) {
                        ExploreScreen(
                            viewModel = viewModel,
                            listState = exploreListState
                        )
                    }
                    MainScreens.Library -> NavEntry(key) {
                        LibraryScreen(
                            viewModel = viewModel,
                            listState = libraryListState
                        )
                    }
                    MainScreens.Stats -> NavEntry(key) {
                        StatsScreen(
                            viewModel = viewModel,
                            scrollState = statsListState
                        )
                    }
                    else -> NavEntry(key) {
                        Text("Unknown route")
                    }
                }
            }

            NavDisplay(
                entries = entries,
                onBack = { navigator.goBack() },
                modifier = Modifier.weight(1f)
            )
        }
    }
    selectedGameForRating?.let { game ->
        GameDetailDialog(
            game = game,
            onDismiss = { viewModel.closeRatingDialog() },
            onSaveRating = { g, rating, status, review, hasPlatinum, hours, isFav, year ->
                viewModel.saveRating(g, rating, status, review, hasPlatinum, hours, isFav, year)
            },
            onRemove = { id ->
                viewModel.removeGame(id)
            },
            relinkResults = relinkResults,
            relinkLoading = relinkLoading,
            onRelinkSearch = viewModel::searchForRelink,
            onRelinkSelect = { target, onResult -> viewModel.relinkGame(game, target, onResult) },
            onRelinkDismiss = viewModel::clearRelinkSearch
        )
    }
}

@Composable
fun TopBarSearchField(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        trailingIcon = {
            IconButton(onClick = { if (query.isNotEmpty()) onQueryChange("") else onClose() }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear_search))
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    )
}
