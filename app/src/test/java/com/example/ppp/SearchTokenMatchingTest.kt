package com.example.ppp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTokenMatchingTest {

    private fun matchesToken(titleLower: String, token: String): Boolean {
        val cleanTitle = titleLower.replace("[^a-z0-9\\s]".toRegex(), " ")
        if (cleanTitle.contains(token) || titleLower.contains(token)) return true

        val equivalent = when (token) {
            "1" -> "i"
            "2" -> "ii"
            "3" -> "iii"
            "4" -> "iv"
            "5" -> "v"
            "6" -> "vi"
            "7" -> "vii"
            "8" -> "viii"
            "9" -> "ix"
            "10" -> "x"
            "11" -> "xi"
            "12" -> "xii"
            "13" -> "xiii"
            "14" -> "xiv"
            "15" -> "xv"
            "16" -> "xvi"
            "17" -> "xvii"
            "18" -> "xviii"
            "19" -> "xix"
            "20" -> "xx"
            "i" -> "1"
            "ii" -> "2"
            "iii" -> "3"
            "iv" -> "4"
            "v" -> "5"
            "vi" -> "6"
            "vii" -> "7"
            "viii" -> "8"
            "ix" -> "9"
            "x" -> "10"
            "xi" -> "11"
            "xii" -> "12"
            "xiii" -> "13"
            "xiv" -> "14"
            "xv" -> "15"
            "xvi" -> "16"
            "xvii" -> "17"
            "xviii" -> "18"
            "xix" -> "19"
            "xx" -> "20"
            else -> null
        }
        return equivalent != null && (cleanTitle.contains(equivalent) || titleLower.contains(equivalent))
    }

    private fun isTitleMatch(title: String, query: String): Boolean {
        val titleLower = title.lowercase()
        val queryTokens = query.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        return queryTokens.all { token -> matchesToken(titleLower, token) }
    }

    @Test
    fun testArabicNumberMatchesRomanNumeral() {
        assertTrue(isTitleMatch("SILENT HILL II", "silent hill 2"))
        assertTrue(isTitleMatch("Silent Hill 2", "silent hill 2"))
        assertTrue(isTitleMatch("The Last of Us Part II", "last of us 2"))
        assertTrue(isTitleMatch("Final Fantasy VII", "final fantasy 7"))
        assertTrue(isTitleMatch("Final Fantasy XV", "final fantasy 15"))
        assertTrue(isTitleMatch("Final Fantasy XVI", "final fantasy 16"))
        assertTrue(isTitleMatch("DRAGON QUEST XX", "dragon quest 20"))
    }

    @Test
    fun testUnrelatedGamesAreFilteredOut() {
        assertFalse(isTitleMatch("God of War", "last of us"))
        assertFalse(isTitleMatch("Silent Hill", "silent hill 2"))
    }

    @Test
    fun testCaseInsensitiveMatching() {
        assertTrue(isTitleMatch("GOD OF WAR RAGNAROK", "god of war"))
        assertTrue(isTitleMatch("astro bot", "Astro Bot"))
    }
}
