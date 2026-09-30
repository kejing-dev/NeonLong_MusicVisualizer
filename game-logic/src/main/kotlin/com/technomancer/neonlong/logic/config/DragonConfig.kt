package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable

/** Tunables for the dragon's flight and body. Part counts are constants in [Companion]. */
@Serializable
data class DragonConfig(
    /** Flight-loop speed in path radians per second, before audio. */
    val speedBase: Float = 0.19f,
    /** Extra speed per unit of overall volume. */
    val speedVol: Float = 0.20f,
    /** Extra speed per unit of bass. */
    val speedBass: Float = 0.10f,
    /** Path length the body trails behind the head, in path radians. */
    val bodyLength: Float = 3.05f,
    /** Spine resolution: the spine has this many steps (one more sample). */
    val spineSamples: Int = 240,
    /** Amplitude of the body's frame roll, in radians. */
    val rollAmp: Float = 0.22f,
    /** Longest simulation step; bigger frame gaps are clamped to this. */
    val maxStepS: Float = 0.05f,
    /** Every part must stay inside this model-space box. */
    val boundsCenterX: Float = 0f,
    val boundsCenterY: Float = 2.5f,
    val boundsCenterZ: Float = 0f,
    val boundsHalfX: Float = 18f,
    val boundsHalfY: Float = 7.5f,
    val boundsHalfZ: Float = 13f,
) {
    companion object {
        const val SEGMENTS = 27
        const val TOWERS = 81
        const val BANDS = 81
        const val BELLY_LIGHTS = 36
        const val SIDE_PLATES = 162
        const val PISTONS = 52
        const val WHISKER_DOTS = 60
        const val MANE_BLADES = 24
        const val LEGS = 4
        const val TAIL_RIBBONS = 18
        const val BREATH_PARTICLES = 700
    }
}
