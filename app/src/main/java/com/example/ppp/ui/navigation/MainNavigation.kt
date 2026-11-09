package com.example.ppp.ui.navigation

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ppp.R
import com.example.ppp.ui.components.ServerStatusBanner
import com.example.ppp.ui.screens.ExploreScreen
import com.example.ppp.ui.screens.GameDetailDialog
import com.example.ppp.ui.screens.LibraryScreen
import com.example.ppp.ui.screens.StatsScreen
import com.example.ppp.ui.theme.PlayStationLightBlue
import com.example.ppp.ui.theme.TextSecondary
import com.example.ppp.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    viewModel: GameViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedGameForRating by viewModel.selectedGameForRating.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val serviceStatus by viewModel.serviceStatus.collectAsState()

    val exploreListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val statsListState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val screens = listOf(
        Screen.Explore,
        Screen.Library,
        Screen.Stats
    )
    val barColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp)
    val isSearchable = currentRoute == Screen.Explore.route || currentRoute == Screen.Library.route
    var isSearchActive by rememberSaveable { mutableStateOf(false) }

// Replace these with your real ViewModel members
    val exploreQuery by viewModel.searchQuery.collectAsState()
    val libraryQuery by viewModel.librarySearchQuery.collectAsState()

    fun closeSearch() {
        isSearchActive = false
        when (currentRoute) {
            Screen.Explore.route -> viewModel.onSearchQueryChanged("")
            Screen.Library.route -> viewModel.onLibrarySearchChanged("")
        }
    }

// Switching tabs closes the search bar and clears the query of the tab you left
    var lastRoute by remember { mutableStateOf(currentRoute) }
    LaunchedEffect(currentRoute) {
        if (lastRoute != currentRoute) {
            isSearchActive = false
            when (lastRoute) {
                Screen.Explore.route -> {
                    viewModel.onSearchQueryChanged("")
                    viewModel.dismissServiceStatus()
                }
                Screen.Library.route -> viewModel.onLibrarySearchChanged("")
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
                                contentDescription = "Close search"
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_controller_logo),
                            contentDescription = "GameBoxd",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
                title = {
                    if (isSearchActive) {
                        if (currentRoute == Screen.Explore.route) {
                            TopBarSearchField(
                                query = exploreQuery,
                                placeholder = "Search all games…",
                                onQueryChange = viewModel::onSearchQueryChanged,
                                onClose = ::closeSearch
                            )
                        } else if (currentRoute == Screen.Library.route) {
                            TopBarSearchField(
                                query = libraryQuery,
                                placeholder = "Search your library…",
                                onQueryChange = viewModel::onLibrarySearchChanged,
                                onClose = ::closeSearch
                            )
                        }
                    } else {
                        Text("GameBoxd")
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        if (isSearchable) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isDarkTheme) Color.White else Color.Black
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.toggleTheme() }) {
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
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch {
                                when (screen) {
                                    Screen.Explore -> exploreListState.scrollToItem(0)
                                    Screen.Library -> libraryListState.scrollToItem(0)
                                    Screen.Stats -> statsListState.scrollTo(0)
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) PlayStationLightBlue else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
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
            NavHost(
                navController = navController,
                startDestination = Screen.Explore.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(Screen.Explore.route) {
                    ExploreScreen(
                        viewModel = viewModel,
                        listState = exploreListState
                    )
                }
                composable(Screen.Library.route) {
                    LibraryScreen(
                        viewModel = viewModel,
                        listState = libraryListState
                    )
                }
                composable(Screen.Stats.route) {
                    StatsScreen(
                        viewModel = viewModel,
                        scrollState = statsListState
                    )
                }
            }
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
            }
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
                Icon(Icons.Default.Close, contentDescription = "Clear search")
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
