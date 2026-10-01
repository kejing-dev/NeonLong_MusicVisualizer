package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable

/** Photosensitivity limits for dragon-wide flashes. None of these can be switched off. */
@Serializable
data class FlashConfig(
    /** At most this many flash onsets in any sliding one-second window. */
    val maxFlashesPerSecond: Int = 3,
    /** And at least this long between onsets. */
    val minOnsetIntervalS: Float = 0.34f,
    /** Flashes ramp up over this long (never a step) and fade with this time constant. */
    val flashAttackS: Float = 0.06f,
    val flashDecayS: Float = 0.25f,
    /** How much a full flash brightens every emissive part. */
    val globalFlashGain: Float = 0.35f,
    /** Flash gain when "Reduce flashing" is on. */
    val reducedFlashGain: Float = 0.10f,
    /** Scale on local beat effects (beacons, eyes, antlers, joint rings) when "Reduce flashing" is on. */
    val reducedLocalPulse: Float = 0.3f,
    /** Extra reduction for the saturated-red Imperial palette. */
    val redGuard: Float = 0.7f,
)
