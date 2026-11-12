package com.example.gameboxd

import com.example.gameboxd.data.repository.matchesToken
import com.example.gameboxd.data.repository.normalizeTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTokenMatchingTest {

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
    fun testRomanNumeralQueryMatchesArabicTitle() {
        assertTrue(isTitleMatch("Final Fantasy 7", "final fantasy vii"))
        assertTrue(isTitleMatch("Resident Evil 4", "resident evil iv"))
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

    @Test
    fun testPunctuationInTitleDoesNotBreakMatching() {
        assertTrue(isTitleMatch("Marvel's Spider-Man", "spider man"))
    }

    @Test
    fun normalizeTitleFoldsCaseAccentsAndPunctuation() {
        assertEquals("pokemonlegendsarceus", normalizeTitle("Pokémon Legends: Arceus"))
        assertEquals("godofwarragnarok", normalizeTitle("God of War Ragnarök"))
        assertEquals(normalizeTitle("Marvel's Spider-Man"), normalizeTitle("MARVELS SPIDER MAN"))
    }
}
