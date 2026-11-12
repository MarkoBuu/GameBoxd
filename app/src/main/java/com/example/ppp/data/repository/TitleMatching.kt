package com.example.ppp.data.repository

import java.text.Normalizer
import java.util.Locale

private val ROMAN_NUMERALS = listOf(
    "1" to "i", "2" to "ii", "3" to "iii", "4" to "iv", "5" to "v",
    "6" to "vi", "7" to "vii", "8" to "viii", "9" to "ix", "10" to "x",
    "11" to "xi", "12" to "xii", "13" to "xiii", "14" to "xiv", "15" to "xv",
    "16" to "xvi", "17" to "xvii", "18" to "xviii", "19" to "xix", "20" to "xx"
)

// Both directions: "7" -> "vii" and "vii" -> "7".
private val NUMERAL_EQUIVALENTS: Map<String, String> =
    ROMAN_NUMERALS.associate { it }.plus(ROMAN_NUMERALS.associate { (arabic, roman) -> roman to arabic })

/** True when [token] appears in the lowercased title, treating arabic and roman numerals as equal. */
fun matchesToken(titleLower: String, token: String): Boolean {
    val cleanTitle = titleLower.replace("[^a-z0-9\\s]".toRegex(), " ")
    if (cleanTitle.contains(token) || titleLower.contains(token)) return true

    val equivalent = NUMERAL_EQUIVALENTS[token] ?: return false
    return cleanTitle.contains(equivalent) || titleLower.contains(equivalent)
}

private val COMBINING_MARKS = "\\p{M}+".toRegex()
private val NON_ALPHANUMERIC = "[^a-z0-9]".toRegex()

/** Lowercase ASCII alphanumerics only (accents folded), used to compare titles across data sources. */
fun normalizeTitle(title: String): String =
    Normalizer.normalize(title, Normalizer.Form.NFD)
        .replace(COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .replace(NON_ALPHANUMERIC, "")
