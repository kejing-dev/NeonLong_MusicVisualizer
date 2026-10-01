package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable

/** One color scheme: a three-stop gradient plus an accent, as "#rrggbb". */
@Serializable
data class PaletteDef(
    val name: String,
    val stop0: String,
    val stop1: String,
    val stop2: String,
    val accent: String,
)

/** The palettes the user can pick, and how long switching between them takes. */
@Serializable
data class PaletteConfig(
    val palettes: List<PaletteDef> = listOf(
        PaletteDef("Kowloon", "#ff2e88", "#a34dff", "#18e7ff", "#ffb627"),
        PaletteDef("Imperial", "#ff2419", "#ff7a14", "#ffc93c", "#3df2ff"),
        PaletteDef("Jade", "#00ffa0", "#12c8d6", "#9d7bff", "#ff5fa8"),
    ),
    val fadeS: Float = 0.4f,
)
