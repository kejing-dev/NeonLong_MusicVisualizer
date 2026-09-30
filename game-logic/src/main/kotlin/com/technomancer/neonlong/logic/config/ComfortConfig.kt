package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable

/** Comfort limits for motion, view modes and turning. Lengths in meters, angles in degrees. */
@Serializable
data class ComfortConfig(
    /** Multiplier on flight speed and spine undulation when Reduce motion is on. */
    val reducedMotionFactor: Float = 0.6f,
    /** Pearl ring rotation multiplier when Reduce motion is on. */
    val reducedMotionPearlRings: Float = 0.3f,
    /** Orbit view in the Stage: meters per model unit and model origin placement. */
    val orbitScale: Float = 0.10f,
    val orbitDistance: Float = 3.6f,
    val orbitHeight: Float = 1.2f,
    /** Ride view in the Stage: meters per model unit, rider offset from the head in model units. */
    val rideScale: Float = 0.40f,
    val rideBack: Float = 6f,
    val rideUp: Float = 2.2f,
    val rideSide: Float = 0f,
    val rideSpeedFactor: Float = 0.5f,
    val rideMaxYawRate: Float = 30f,
    val rideYawSmoothingS: Float = 0.6f,
    val rideMaxVerticalSpeed: Float = 0.5f,
    val rideMaxSpeed: Float = 1.2f,
    val rideVignetteMin: Float = 0.25f,
    val rideVignetteMax: Float = 0.7f,
    val rideVignetteInnerRadius: Float = 0.55f,
    /** Seated mode lowers the Stage content by this much. */
    val seatedHeightOffset: Float = -0.45f,
    val snapTurnDeg: Float = 45f,
    val snapTurnThreshold: Float = 0.7f,
    val snapTurnRearm: Float = 0.3f,
    val viewpointRadiusM: Float = 5.0f,
    /** Fade through black when opening or closing the Stage, and when jumping viewpoints. */
    val stageFadeS: Float = 0.4f,
    val viewpointFadeS: Float = 0.2f,
    /** Head-locked toasts: longest time on screen, angle below gaze, distance. */
    val toastMaxS: Float = 3.8f,
    val toastBelowGazeDeg: Float = 15f,
    val toastDistance: Float = 1.2f,
)
