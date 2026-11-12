package com.example.ppp

import com.example.ppp.data.local.GameEntity
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.playStationOnly
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModelTest {

    @Test
    fun playStationOnlyDropsOtherPlatforms() {
        val platforms = listOf("PC (Microsoft Windows)", "PlayStation 4", "Xbox One", "PlayStation 5")

        assertEquals(listOf("PlayStation 4", "PlayStation 5"), platforms.playStationOnly())
    }

    @Test
    fun playStationOnlyFallsBackToAGenericLabel() {
        assertEquals(listOf("PlayStation"), listOf("PC (Microsoft Windows)").playStationOnly())
        assertEquals(listOf("PlayStation"), emptyList<String>().playStationOnly())
    }

    @Test
    fun playStationOnlyKeepsHandheldsAndVr() {
        val platforms = listOf("PlayStation Portable", "PlayStation Vita", "PlayStation VR2")

        assertEquals(platforms, platforms.playStationOnly())
    }

    @Test
    fun storedPlatformsAreCleanedWhenLoadedFromTheDatabase() {
        val entity = GameEntity.fromGame(savedGame(1, "Game")).copy(platforms = "PC (Microsoft Windows),PlayStation 4,Xbox One")

        assertEquals(listOf("PlayStation 4"), entity.toGame().platforms)
    }

    @Test
    fun blankStoredPlatformsBecomeTheGenericLabel() {
        val entity = GameEntity.fromGame(savedGame(1, "Game")).copy(platforms = "")

        assertEquals(listOf("PlayStation"), entity.toGame().platforms)
    }

    @Test
    fun ratingIsStillSerializedAsRawgRatingSoOldBackupsAndNewBackupsInterchange() {
        val json = Json.encodeToString(Game.serializer(), Game(id = 1, title = "G", igdbRating = 4.2))

        assertTrue(json.contains("\"rawgRating\":4.2"))
        assertEquals(4.2, Json.decodeFromString(Game.serializer(), json).igdbRating, 0.0001)
    }

    @Test
    fun entityRoundTripPreservesEveryUserField() {
        val game = savedGame(7, "Round Trip", favorite = true, rating = 4.5f)

        assertEquals(game, GameEntity.fromGame(game).toGame())
    }
}
