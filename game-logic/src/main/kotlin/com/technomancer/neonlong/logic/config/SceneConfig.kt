package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable

/** Size and placement of the dragon volume and the items fixed to it. Lengths in meters. */
@Serializable
data class SceneConfig(
    /** Distance in front of the user where the volume opens. */
    val spawnDistance: Float = 1.5f,
    /** Vertical offset of the volume's center from eye height. */
    val spawnHeightOffset: Float = -0.15f,
    val defaultWidth: Float = 1.2f,
    val defaultHeight: Float = 0.47f,
    val defaultDepth: Float = 0.87f,
    val minWidth: Float = 0.5f,
    val maxWidth: Float = 3.0f,
    /** Model units spanning the volume's width; the dragon's scale is width / this. */
    val modelUnitsAcross: Float = 36f,
    /** Turntable oval radii as a fraction of the volume's width (across) and depth (front to back). */
    val turntableRadiusFrac: Float = 0.45f,
    /** Degrees of dragon yaw per degree of hand arc on the turntable. */
    val turntableGain: Float = 1.0f,
    /** Below this volume width, small parts are hidden. */
    val lodSmallWidth: Float = 0.7f,
)
