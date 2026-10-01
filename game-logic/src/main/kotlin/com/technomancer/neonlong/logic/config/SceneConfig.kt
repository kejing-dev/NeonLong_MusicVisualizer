package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable

/** Size and placement of the dragon volume and the items fixed to it. Lengths in meters. */
@Serializable
data class SceneConfig(
    /** Distance in front of the user where the volume opens. */
    val spawnDistance: Float = 1.5f,
    /** Vertical offset of the volume's center from eye height. */
    val spawnHeightOffset: Float = -0.15f,
    /**
     * Default volume: the model box (50 × 24 × 42 units) at 1.2 / 36 m per unit, the dragon's
     * original scale, so every part fits inside the volume at full input.
     */
    val defaultWidth: Float = 1.6667f,
    val defaultHeight: Float = 0.8f,
    val defaultDepth: Float = 1.4f,
    /** Width limits, scaled with the box so the dragon's own size range is unchanged (0.5–3.0 m for 36 units). */
    val minWidth: Float = 0.6944f,
    val maxWidth: Float = 4.1667f,
    /** Model units spanning the volume's width; the dragon's scale is width / this. */
    val modelUnitsAcross: Float = 50f,
    /** Turntable oval radii as a fraction of the volume's width (across) and depth (front to back). */
    val turntableRadiusFrac: Float = 0.45f,
    /** Degrees of dragon yaw per degree of hand arc on the turntable. */
    val turntableGain: Float = 1.0f,
    /** Below this volume width, small parts are hidden. */
    val lodSmallWidth: Float = 0.9722f,
)
