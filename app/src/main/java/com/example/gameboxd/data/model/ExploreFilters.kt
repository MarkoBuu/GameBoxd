package com.example.gameboxd.data.model

/** Platform chips on the Explore screen. A null id list means "all PlayStation platforms". */
enum class PlatformFilter(val label: String, val platformIds: List<Int>?) {
    ALL("All", null),
    PS5("PS5", listOf(167)),
    PS4("PS4", listOf(48)),
    PS3("PS3", listOf(9)),
    PS2("PS2", listOf(8)),
    PS1("PS1", listOf(7)),
    HANDHELD("PSP / Vita", listOf(38, 46)),
    VR("VR", listOf(165, 390))
}

/**
 * Genre chips on the Explore screen, mapped to IGDB genre ids.
 * IGDB has no plain "Action" genre, so "Hack & Slash" stands in for the closest one.
 */
enum class ExploreGenre(val label: String, val genreIds: List<Int>?) {
    ALL("All", null),
    RPG("RPG", listOf(12)),
    ADVENTURE("Adventure", listOf(31)),
    SHOOTER("Shooter", listOf(5)),
    INDIE("Indie", listOf(32)),
    STRATEGY("Strategy", listOf(15, 16, 11)),
    PLATFORMER("Platformer", listOf(8)),
    PUZZLE("Puzzle", listOf(9)),
    SPORTS("Sports", listOf(14)),
    RACING("Racing", listOf(10)),
    FIGHTING("Fighting", listOf(4)),
    SIMULATOR("Simulator", listOf(13)),
    HACK_AND_SLASH("Hack & Slash", listOf(25))
}
