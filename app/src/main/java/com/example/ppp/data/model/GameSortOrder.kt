package com.example.ppp.data.model

import androidx.annotation.StringRes
import com.example.ppp.R

enum class GameSortOrder(val displayName: String, val apiValue: String, @StringRes val displayNameRes: Int) {
    RATING_DESC("Top Rated", "-rating", R.string.sort_top_rated),
    RELEASE_DATE_DESC("Newest First", "-released", R.string.sort_newest_first),
    RELEASE_DATE_ASC("Oldest First", "released", R.string.sort_oldest_first),
    NAME_ASC("Name (A-Z)", "name", R.string.sort_name_asc),
    NAME_DESC("Name (Z-A)", "-name", R.string.sort_name_desc)
}
