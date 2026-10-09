package com.example.ppp.data.model

import androidx.annotation.StringRes
import com.example.ppp.R

enum class LibrarySortOption(val displayName: String, @StringRes val displayNameRes: Int) {
    RECENTLY_UPDATED("Recently Updated", R.string.sort_recently_updated),
    HIGHEST_RATING("Highest Rating", R.string.sort_highest_rating),
    RELEASE_DATE("Release Date", R.string.sort_newest_first),
    NAME("Name (A-Z)", R.string.sort_name_asc),
    MOST_PLAYED("Most Played", R.string.sort_most_played)
}
