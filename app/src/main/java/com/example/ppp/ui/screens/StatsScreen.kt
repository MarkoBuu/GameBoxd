package com.example.ppp.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ppp.R
import com.example.ppp.data.model.PlayStatus
import com.example.ppp.ui.components.SortDropdown
import com.example.ppp.ui.theme.AccentGold
import com.example.ppp.ui.theme.PlayStationLightBlue
import com.example.ppp.ui.theme.TextSecondary
import com.example.ppp.ui.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("DefaultLocale", "UnusedBoxWithConstraintsScope")
@Composable
fun StatsScreen(
    viewModel: GameViewModel,
    scrollState: ScrollState = rememberScrollState()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val savedGames by viewModel.savedGames.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var selectedStatsYear by remember { mutableStateOf<Int?>(null) }
    val yearOptions: List<Int?> = listOf<Int?>(null) + availableYears

    val yearlyGames = if (selectedStatsYear == null) savedGames else savedGames.filter { it.completionYear == selectedStatsYear }

    val totalRated = yearlyGames.size
    val totalPlatinums =
        yearlyGames.count { it.hasPlatinum && it.playStatus == PlayStatus.COMPLETED }
    val totalFavorites = yearlyGames.count { it.isFavorite }
    val totalHours =
        yearlyGames.filter { it.playStatus != PlayStatus.BACKLOG }.sumOf { it.playtimeHours }
    val completedGames = yearlyGames.filter { it.playStatus == PlayStatus.COMPLETED }
    val droppedGames = yearlyGames.filter { it.playStatus == PlayStatus.DROPPED }
    val completedCount = completedGames.size
    val backlogCount = yearlyGames.count { it.playStatus == PlayStatus.BACKLOG }
    val droppedCount = droppedGames.size

    val attemptedCount = completedCount + droppedCount
    val completionRate =
        if (attemptedCount > 0) (completedCount * 100f / attemptedCount).roundToInt() else null

    val ratedGames = yearlyGames.filter { it.playStatus.allowsRating && it.userRating > 0f }
    val averageScore: Float? = if (ratedGames.isNotEmpty()) {
        ratedGames.map { it.userRating }.average().toFloat()
    } else null
    val ratingBuckets = (1..10).map { step ->
        val value = step / 2f
        value to ratedGames.count { (it.userRating * 2).roundToInt().coerceIn(1, 10) == step }
    }
    val maxBucket = ratingBuckets.maxOfOrNull { it.second } ?: 0
    val topRated = ratedGames.maxByOrNull { it.userRating }
    val mostPlayed = yearlyGames.filter { it.playtimeHours > 0 }.maxByOrNull { it.playtimeHours }
    val platinumRate = if (completedCount > 0) totalPlatinums * 100 / completedCount else null

    val avgCompletionHours = if (completedCount > 0) {
        val completedHoursTotal = completedGames.sumOf { it.playtimeHours }
        if (completedHoursTotal > 0) (completedHoursTotal.toFloat() / completedCount).roundToInt() else null
    } else null

    val toastJsonCopied = stringResource(R.string.toast_json_copied)
    val toastImportSummary = stringResource(R.string.toast_import_summary)
    val toastInvalidJson = stringResource(R.string.toast_invalid_json)

    val showScrollToTop by remember {
        derivedStateOf { scrollState.value > 300 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.title_stats),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                SortDropdown(
                    showText = false,
                    options = yearOptions,
                    selected = selectedStatsYear,
                    optionLabel = { yr -> yr?.let { "$it" } ?: stringResource(R.string.stats_all_time) },
                    onSelected = { selectedStatsYear = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stat_in_library),
                    value = "$totalRated",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.stat_avg_rating),
                    value = averageScore?.let { String.format("%.1f", it) } ?: "N/A",
                    modifier = Modifier.weight(1f),
                    valueColor = AccentGold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stat_playtime),
                    value = "${totalHours}h",
                    modifier = Modifier.weight(1f),
                    valueColor = PlayStationLightBlue
                )
                StatCard(
                    title = stringResource(R.string.stat_platinums),
                    value = "$totalPlatinums",
                    modifier = Modifier.weight(1f),
                    valueColor = AccentGold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.library_details_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow(label = stringResource(R.string.stat_total_games), count = totalRated)
                    StatusRow(label = stringResource(R.string.status_completed), count = completedCount)
                    StatusRow(label = stringResource(R.string.status_backlog), count = backlogCount)
                    StatusRow(label = stringResource(R.string.status_dropped), count = droppedCount)
                    StatusRow(label = stringResource(R.string.stat_favorites), count = totalFavorites)
                    completionRate?.let { rate ->
                        StatusTextRow(
                            label = stringResource(R.string.stat_completion_rate),
                            value = "$rate%"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (ratedGames.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.rating_breakdown_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.rated_count_label, ratedGames.size),
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val showHalfLabels = maxWidth >= 300.dp
                            val maxBarHeight = 48.dp

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                ratingBuckets.forEach { (value, count) ->
                                    val isWhole = value % 1f == 0f
                                    val fraction =
                                        if (maxBucket > 0) count / maxBucket.toFloat() else 0f
                                    val barHeight = if (count == 0) 2.dp
                                    else (maxBarHeight * fraction).coerceAtLeast(6.dp)

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(maxBarHeight + 28.dp),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                if (count > 0) {
                                                    Text(
                                                        text = "$count",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(barHeight)
                                                        .clip(
                                                            RoundedCornerShape(
                                                                topStart = 3.dp,
                                                                topEnd = 3.dp
                                                            )
                                                        )
                                                        .background(
                                                            when {
                                                                count == 0 -> MaterialTheme.colorScheme.surfaceVariant
                                                                isWhole -> AccentGold
                                                                else -> AccentGold.copy(alpha = 0.55f)
                                                            }
                                                        )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = when {
                                                isWhole -> "${value.toInt()}"
                                                showHalfLabels -> "$value"
                                                else -> ""
                                            },
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            softWrap = false,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.heightIn(min = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (topRated != null || mostPlayed != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.highlights_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        topRated?.let {
                            HighlightRow(
                                stringResource(R.string.highlight_top_rated), it.title,
                                String.format("%.1f ★", it.userRating), AccentGold
                            )
                        }
                        mostPlayed?.let {
                            HighlightRow(
                                stringResource(R.string.highlight_most_played),
                                it.title,
                                "${it.playtimeHours}h",
                                PlayStationLightBlue
                            )
                        }
                        avgCompletionHours?.let { avgHours ->
                            HighlightRow(
                                stringResource(R.string.highlight_avg_completion_time),
                                stringResource(R.string.per_completed_game),
                                "${avgHours}h",
                                PlayStationLightBlue
                            )
                        }
                        platinumRate?.let {
                            HighlightRow(
                                stringResource(R.string.highlight_platinum_rate),
                                stringResource(R.string.of_completed_games),
                                "$it%",
                                AccentGold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // Backup & Data Export Options
            OutlinedButton(
                onClick = { showBackupDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlayStationLightBlue)
            ) {
                Text(
                    text = stringResource(R.string.backup_restore_btn),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
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
                        scrollState.animateScrollTo(0)
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

    if (showBackupDialog) {
        Dialog(onDismissRequest = { showBackupDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.backup_dialog_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.exportDataJson { json ->
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(
                                    context,
                                    toastJsonCopied,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PlayStationLightBlue)
                    ) {
                        Text(stringResource(R.string.btn_copy_json))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        label = {
                            Text(
                                stringResource(R.string.paste_json_label),
                                color = TextSecondary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PlayStationLightBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (importJsonText.isNotBlank()) {
                                viewModel.importDataJson(importJsonText) { result ->
                                    if (result != null) {
                                        Toast.makeText(
                                            context,
                                            toastImportSummary.format(
                                                result.added,
                                                result.updated,
                                                result.skipped
                                            ),
                                            Toast.LENGTH_LONG
                                        ).show()
                                        showBackupDialog = false
                                    } else {
                                        Toast.makeText(
                                            context,
                                            toastInvalidJson,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                    ) {
                        Text(
                            stringResource(R.string.btn_restore_json),
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showBackupDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.btn_close))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = PlayStationLightBlue
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(text = value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun StatusRow(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = TextSecondary)
        Text(
            text = "$count",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusTextRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PlayStationLightBlue
        )
    }
}

@Composable
private fun HighlightRow(label: String, title: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = TextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }
        }
    }
}
