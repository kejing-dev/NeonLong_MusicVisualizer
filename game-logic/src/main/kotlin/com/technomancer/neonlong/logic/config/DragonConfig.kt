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
    /**
     * Every part must stay inside this model-space box. Widened from the original 18 × 7.5 × 13
     * half-extents so the tail, legs and tower tops fit at full input; the dragon's size and the
     * volume's scale (width / [SceneConfig.modelUnitsAcross]) are unchanged.
     */
    val boundsCenterX: Float = 0f,
    val boundsCenterY: Float = 2.5f,
    val boundsCenterZ: Float = 0f,
    val boundsHalfX: Float = 25f,
    val boundsHalfY: Float = 12f,
    val boundsHalfZ: Float = 21f,
    /** Light waves along the body: speed in spine units per second, Gaussian width d², lifetime. */
    val waveSpeed: Float = 1.35f,
    val waveWidth: Float = 0.0035f,
    val waveLifeS: Float = 1.6f,
    val waveMaxStrength: Float = 1.4f,
    /** Touch ripples: strength, speed, extra glow on the touched segment and its decay rate, cooldown. */
    val rippleStrength: Float = 1.0f,
    val rippleSpeed: Float = 1.0f,
    val rippleFlash: Float = 1.5f,
    val rippleFlashDecay: Float = 4f,
    val rippleCooldownS: Float = 0.25f,
    /** Call: fraction the flight loop shifts toward the user, how long the pearl stays parked, blend rate. */
    val callOffset: Float = 0.3f,
    val pearlParkS: Float = 5.0f,
    val callBlendRate: Float = 2.0f,
    /** Where the loop is pulled toward on a Call, along +Z (model units). */
    val callFrontZ: Float = 11.5f,
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
