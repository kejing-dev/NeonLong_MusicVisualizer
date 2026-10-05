package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable

/**
 * How the neon is made to read as glowing without bloom: an overall brightness lift, soft
 * additive halos around the brightest parts, a glow sleeve around the energy core, and
 * colored lights that ride the body. Sizes are multiples of the part's own size.
 */
@Serializable
data class GlowConfig(
    /** Brightness lift applied to every glow before it's capped. */
    val boost: Float = 1.4f,
    /** Halo size and strength per part. */
    val beaconHaloSize: Float = 4.0f,
    val beaconHaloGain: Float = 0.6f,
    val bellyHaloSize: Float = 2.6f,
    val bellyHaloGain: Float = 0.5f,
    val eyeHaloSize: Float = 3.2f,
    val eyeHaloGain: Float = 0.7f,
    val pearlHaloSize: Float = 3.6f,
    val pearlHaloGain: Float = 0.7f,
    val jointHaloSize: Float = 2.4f,
    val jointHaloGain: Float = 0.45f,
    /** Halos grow from this share of their size (dark) to full size (brightest). */
    val haloMinScale: Float = 0.6f,
    /** Glow sleeve around the energy core: radius multiple and strength. The core is about half the body's radius, so the sleeve must stay under 1.9 to remain inside the blocks. */
    val coreSleeveRadius: Float = 1.45f,
    val coreSleeveGain: Float = 0.4f,
    /** Colored point lights along the body: spine positions, strength and reach in model units. */
    val bodyLightU: List<Float> = listOf(0.08f, 0.4f, 0.75f),
    val bodyLightIntensity: Float = 2.0f,
    val bodyLightRadius: Float = 26f,
    val pearlLightIntensity: Float = 2.5f,
    val pearlLightRadius: Float = 22f,
)
