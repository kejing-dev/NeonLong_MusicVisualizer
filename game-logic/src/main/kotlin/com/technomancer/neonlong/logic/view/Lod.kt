package com.technomancer.neonlong.logic.view

import com.technomancer.neonlong.logic.config.SceneConfig

/** Which small parts to draw for a given volume width. */
class Lod(private val scene: SceneConfig) {
    /** Pistons, cables and joint rings are hidden in small volumes. */
    fun showSmallParts(widthMeters: Float): Boolean = widthMeters >= scene.lodSmallWidth

    /** Draw every whisker dot, or every other one in small volumes. */
    fun whiskerStride(widthMeters: Float): Int = if (showSmallParts(widthMeters)) 1 else 2
}
