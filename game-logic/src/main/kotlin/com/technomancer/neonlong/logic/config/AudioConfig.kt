package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable

/** Spectrum analysis, band features and beat detection settings. */
@Serializable
data class AudioConfig(
    val fftSize: Int = 4096,
    val analysisRateHz: Float = 60f,
    /** Weight of the previous magnitude when smoothing each FFT bin. */
    val smoothing: Float = 0.7f,
    val minDb: Float = -90f,
    val maxDb: Float = -22f,
    /** Band follower rates, per second: fast rise, slower fall. */
    val attackRate: Float = 32f,
    val releaseRate: Float = 7f,
    /** Beat detector: average-follow rate, threshold ratio and offset, floor, minimum spacing. */
    val beatAvgRate: Float = 2.5f,
    val beatRatio: Float = 1.16f,
    val beatOffset: Float = 0.035f,
    val beatFloor: Float = 0.22f,
    val beatMinInterval: Float = 0.27f,
    val beatStrengthBoost: Float = 0.25f,
    val beatPulseDecay: Float = 6f,
    /** Idle breathing when nothing plays. */
    val idleBpm: Float = 96f,
    /** Default band sensitivity (the user can set 0.5–2.0). */
    val sensitivity: Float = 1.0f,
)
