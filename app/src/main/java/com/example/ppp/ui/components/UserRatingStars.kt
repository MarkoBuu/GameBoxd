package com.example.ppp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ppp.ui.theme.AccentGold
import java.util.Locale

@Composable
fun UserRatingStars(
    rating: Float,
    starSize: Dp = 14.dp,
) {
    if (rating <= 0f) return

    val formattedScore = String.format(Locale.US, "%.1f", rating)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentGold.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = AccentGold,
            modifier = Modifier.size(starSize)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$formattedScore / 10",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGold
        )
    }
}
