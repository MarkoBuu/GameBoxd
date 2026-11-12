package com.example.ppp.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ppp.R
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.ui.components.RatingStars
import com.example.ppp.ui.theme.AccentGold
import com.example.ppp.ui.theme.PlayStationLightBlue
import com.example.ppp.ui.theme.TextSecondary
import com.example.ppp.ui.utils.shareReviewCardImage
import kotlinx.coroutines.launch

private const val MIN_VALID_YEAR = 2000

@Composable
private fun FixMatchDialog(
    initialQuery: String,
    results: List<Game>,
    loading: Boolean,
    onSearch: (String) -> Unit,
    onSelect: (Game) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    // Start from the current title so the right entry is usually already in the list.
    LaunchedEffect(Unit) { onSearch(initialQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fix_match_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.fix_match_hint),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        onSearch(it)
                    },
                    label = { Text(stringResource(R.string.fix_match_search_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        loading -> CircularProgressIndicator(color = PlayStationLightBlue)
                        results.isEmpty() -> Text(
                            text = stringResource(R.string.fix_match_no_results),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(results, key = { it.id }) { candidate ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelect(candidate) }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = candidate.title,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = listOfNotNull(
                                            candidate.releaseDate?.take(4),
                                            candidate.platforms.joinToString(", ")
                                        ).joinToString(" • "),
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameDetailDialog(
    game: Game,
    onDismiss: () -> Unit,
    onSaveRating: (game: Game, rating: Float, status: PlayStatus, review: String, hasPlatinum: Boolean, playtimeHours: Int, isFavorite: Boolean, completionYear: Int?) -> Unit,
    onRemove: (gameId: Int) -> Unit,
    relinkResults: List<Game> = emptyList(),
    relinkLoading: Boolean = false,
    onRelinkSearch: (String) -> Unit = {},
    onRelinkSelect: (target: Game, onResult: (Boolean) -> Unit) -> Unit = { _, _ -> },
    onRelinkDismiss: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val isAlreadySaved = game.playStatus != PlayStatus.NONE

    val initialRating = if (game.userRating > 0f) game.userRating else 0f
    val initialStatus = if (isAlreadySaved) game.playStatus else PlayStatus.COMPLETED
    val initialReview = game.reviewText
    val initialPlatinum = game.hasPlatinum
    val initialPlaytimeInput = if (game.playtimeHours > 0) game.playtimeHours.toString() else ""
    val initialFavorite = game.isFavorite && initialStatus == PlayStatus.COMPLETED
    val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
    val initialYearInput =
        game.completionYear?.takeIf { it > 0 }?.toString() ?: currentYear.toString()

    var userRating by remember { mutableFloatStateOf(initialRating) }
    var selectedStatus by remember { mutableStateOf(initialStatus) }
    var reviewText by remember { mutableStateOf(initialReview) }
    var hasPlatinum by remember { mutableStateOf(initialPlatinum) }
    var playtimeInput by remember { mutableStateOf(initialPlaytimeInput) }
    var isFavorite by remember { mutableStateOf(initialFavorite) }
    var yearInput by remember { mutableStateOf(initialYearInput) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showFixMatchDialog by remember { mutableStateOf(false) }

    val platinumEnabled = selectedStatus == PlayStatus.COMPLETED
    val detailsEnabled = selectedStatus != PlayStatus.BACKLOG
    // Ratings, reviews, and favorites are allowed ONLY for COMPLETED games
    val isCompleted = selectedStatus == PlayStatus.COMPLETED

    // Share is available ONLY if the game is in your library and is COMPLETED
    val canShareReview = isAlreadySaved && selectedStatus == PlayStatus.COMPLETED

    val effectiveRating = if (isCompleted) userRating else 0f
    val effectivePlatinum = hasPlatinum && platinumEnabled
    val effectiveFavorite = isFavorite && isCompleted
    val effectiveReview = if (isCompleted && effectiveRating > 0f) reviewText else ""
    val effectiveHours = if (detailsEnabled) playtimeInput else ""
    val effectiveYear = if (detailsEnabled) {
        yearInput.toIntOrNull()?.takeIf { it in MIN_VALID_YEAR..currentYear } ?: currentYear
    } else {
        null
    }

    val hasChanges = userRating != initialRating ||
            selectedStatus != initialStatus ||
            effectiveReview != initialReview ||
            effectivePlatinum != initialPlatinum ||
            effectiveHours != initialPlaytimeInput ||
            effectiveFavorite != initialFavorite ||
            effectiveYear != game.completionYear

    val isButtonEnabled = (!isAlreadySaved || hasChanges)
            && (selectedStatus != PlayStatus.COMPLETED || effectiveRating > 0f)
    val buttonText = if (isAlreadySaved) stringResource(R.string.btn_update_entry)
    else stringResource(R.string.btn_add_to_library)

    val statusOptions = listOf(
        PlayStatus.COMPLETED,
        PlayStatus.BACKLOG,
        PlayStatus.DROPPED
    )
    val context = LocalContext.current

    val toastSavedText = stringResource(R.string.toast_saved)
    val toastUpdatedText = stringResource(R.string.toast_updated)
    val toastRemovedText = stringResource(R.string.toast_removed)

    fun performShare() {
        coroutineScope.launch {
            shareReviewCardImage(
                context = context,
                game = game,
                rating = effectiveRating,
                status = selectedStatus,
                review = effectiveReview,
                hasPlatinum = effectivePlatinum,
                playtimeHours = effectiveHours.toIntOrNull() ?: 0,
                completionYear = effectiveYear
            )
        }
    }

    val fixMatchDoneText = stringResource(R.string.fix_match_done)
    val fixMatchConflictText = stringResource(R.string.fix_match_conflict)
    val screenContext = LocalContext.current
    if (showFixMatchDialog) {
        FixMatchDialog(
            initialQuery = game.title,
            results = relinkResults,
            loading = relinkLoading,
            onSearch = onRelinkSearch,
            onSelect = { target ->
                onRelinkSelect(target) { success ->
                    Toast.makeText(
                        screenContext,
                        if (success) fixMatchDoneText else fixMatchConflictText,
                        Toast.LENGTH_SHORT
                    ).show()
                    if (success) showFixMatchDialog = false
                }
            },
            onDismiss = {
                showFixMatchDialog = false
                onRelinkDismiss()
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    stringResource(R.string.remove_game_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(R.string.remove_game_confirm, game.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onRemove(game.id)
                        Toast.makeText(context, toastRemovedText, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(stringResource(R.string.btn_remove), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .safeDrawingPadding()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Rich Full-Screen Hero Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    AsyncImage(
                        model = game.backgroundUrl ?: game.coverUrl,
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Top Action Bar Overlay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorites Heart Toggle - Only for Completed Games!
                        if (isCompleted) {
                            IconButton(
                                onClick = { isFavorite = !isFavorite },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite
                                    else Icons.Default.FavoriteBorder,
                                    contentDescription = stringResource(R.string.favorite_content_desc),
                                    tint = if (isFavorite) Color(0xFFFF4081) else Color.White
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Share Review Card Image Button - Available ONLY if in library and COMPLETED!
                            if (canShareReview) {
                                IconButton(
                                    onClick = { performShare() },
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = stringResource(R.string.share_review_btn),
                                        tint = Color.White
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close_content_description),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Bottom Title Overlay in Hero Banner
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = game.coverUrl,
                            contentDescription = game.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = game.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2
                            )
                            Text(
                                text = stringResource(
                                    R.string.release_date_format,
                                    game.releaseDate ?: stringResource(R.string.release_date_na)
                                ),
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (game.genres.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(
                                6.dp,
                                Alignment.CenterHorizontally
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            game.genres.forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = PlayStationLightBlue
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        statusOptions.forEach { status ->
                            val isSelected = selectedStatus == status
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedStatus = status },
                                label = {
                                    Text(
                                        text = status.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PlayStationLightBlue,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Year Completed / Year Dropped OutlinedTextField
                    AnimatedVisibility(
                        visible = detailsEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(modifier = Modifier.padding(bottom = 12.dp)) {
                            OutlinedTextField(
                                value = yearInput,
                                onValueChange = { input ->
                                    if (input.isEmpty()) {
                                        yearInput = input
                                    } else if (input.length <= 4 && input.all { it.isDigit() } && input.startsWith(
                                            "2"
                                        )) {
                                        val minYearForPrefix = input.padEnd(4, '0').toIntOrNull()
                                        if (minYearForPrefix != null && minYearForPrefix <= currentYear) {
                                            yearInput = input
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (selectedStatus == PlayStatus.COMPLETED) stringResource(
                                            R.string.year_completed_label
                                        ) else stringResource(R.string.year_dropped_label),
                                        color = TextSecondary
                                    )
                                },
                                placeholder = { Text("E.g. $currentYear") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PlayStationLightBlue,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }

                    // Animated Platinum Trophy Chip (Only for Completed games)
                    AnimatedVisibility(
                        visible = platinumEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(modifier = Modifier.padding(bottom = 12.dp)) {
                            FilterChip(
                                selected = effectivePlatinum,
                                onClick = { hasPlatinum = !hasPlatinum },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(R.string.platinum_trophy),
                                        fontSize = 11.sp,
                                        fontWeight = if (effectivePlatinum) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = MaterialTheme.colorScheme.surface,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.surface,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = TextSecondary,
                                    iconColor = TextSecondary
                                )
                            )
                        }
                    }

                    // Rating Stars - ONLY FOR COMPLETED GAMES!
                    AnimatedVisibility(
                        visible = isCompleted,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.your_rating),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (effectiveRating > 0f) "${
                                        String.format(
                                            "%.1f",
                                            effectiveRating
                                        )
                                    } / 5.0" else stringResource(R.string.tap_stars_to_rate),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            RatingStars(
                                rating = effectiveRating,
                                starSize = 32.dp,
                                interactive = true,
                                onRatingChanged = { userRating = it }
                            )
                        }
                    }

                    // Playtime Input + Community Playtime Comparison Badge
                    AnimatedVisibility(
                        visible = detailsEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            OutlinedTextField(
                                value = effectiveHours,
                                onValueChange = { input ->
                                    if (input.isEmpty() || (input.length <= 4 && input.all { it.isDigit() })) {
                                        playtimeInput = input
                                    }
                                },
                                label = {
                                    Text(
                                        stringResource(R.string.hours_played),
                                        color = TextSecondary
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PlayStationLightBlue,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            if (game.averagePlaytime > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.community_avg_playtime_format,
                                        game.averagePlaytime
                                    ),
                                    fontSize = 11.sp,
                                    color = PlayStationLightBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Review Notes Input - Allowed ONLY for COMPLETED games!
                    AnimatedVisibility(
                        visible = isCompleted,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val isReviewAllowed = effectiveRating > 0f
                        Box(modifier = Modifier.padding(bottom = 12.dp)) {
                            OutlinedTextField(
                                value = if (isReviewAllowed) reviewText else "",
                                onValueChange = { if (isReviewAllowed) reviewText = it },
                                enabled = isReviewAllowed,
                                label = {
                                    Text(
                                        stringResource(R.string.write_review_label),
                                        color = TextSecondary
                                    )
                                },
                                supportingText = {
                                    if (!isReviewAllowed) {
                                        Text(
                                            text = stringResource(R.string.review_rating_required_hint),
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PlayStationLightBlue,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledBorderColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    ),
                                    disabledLabelColor = TextSecondary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val hours = effectiveHours.toIntOrNull() ?: 0
                            onSaveRating(
                                game,
                                effectiveRating,
                                selectedStatus,
                                effectiveReview,
                                effectivePlatinum,
                                hours,
                                effectiveFavorite,
                                effectiveYear
                            )
                            Toast.makeText(
                                context,
                                if (isAlreadySaved) toastUpdatedText else toastSavedText,
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        enabled = isButtonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlayStationLightBlue,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = buttonText,
                            fontWeight = FontWeight.Bold,
                            color = if (isButtonEnabled) MaterialTheme.colorScheme.onPrimary else TextSecondary
                        )
                    }

                    // Share Review Button - Available ONLY if in library and COMPLETED!
                    if (canShareReview) {
                        OutlinedButton(
                            onClick = { performShare() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PlayStationLightBlue)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.share_review_btn),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isAlreadySaved) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(text = stringResource(R.string.btn_remove_from_library))
                        }
                        TextButton(
                            onClick = { showFixMatchDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.btn_fix_match),
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
