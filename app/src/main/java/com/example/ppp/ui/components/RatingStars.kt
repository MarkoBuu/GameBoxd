package com.example.ppp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ppp.R
import com.example.ppp.ui.theme.AccentGold

@Composable
fun RatingStars(
    rating: Float,
    maxStars: Int = 5,
    starSize: Dp = 18.dp,
    interactive: Boolean = false,
    onRatingChanged: ((Float) -> Unit)? = null
) {
    Row {
        for (i in 1..maxStars) {
            val starValue = i.toFloat()
            val icon = when {
                rating >= starValue -> Icons.Filled.Star
                rating >= starValue - 0.5f -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }

            Box(
                modifier = Modifier
                    .size(starSize)
                    .padding(end = 2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.rating_stars_content_description, i),
                    tint = if (rating >= starValue - 0.5f) AccentGold else Color.Gray,
                    modifier = Modifier.matchParentSize()
                )

                if (interactive && onRatingChanged != null) {
                    Row(modifier = Modifier.matchParentSize()) {
                        // Left half of star -> i - 0.5f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onRatingChanged(starValue - 0.5f)
                                }
                        )

                        // Right half of star -> i.0f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onRatingChanged(starValue)
                                }
                        )
                    }
                }
            }
        }
    }
}
