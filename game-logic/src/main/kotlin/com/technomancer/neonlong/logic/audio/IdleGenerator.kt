package com.technomancer.neonlong.logic.audio

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.math.Easing
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/**
 * Keeps the dragon breathing when nothing plays: slow sines across the bands plus a soft
 * 96 BPM heartbeat in the low bands. Never produces beats.
 */
class IdleGenerator(private val bpm: Float = 96f) {
    /** Writes band values in [0, 0.8] for time [t] into [out]. */
    fun fill(t: Float, out: FloatArray) {
        val beats = t * bpm / 60f
        val phase = beats - floor(beats)
        val hb = exp(-7f * phase)
        for (i in 0 until DragonConfig.BANDS) {
            val v = 0.16f + 0.12f * sin(1.3f * t + 0.23f * i) + 0.08f * sin(2.7f * t - 0.11f * i) +
                hb * 0.45f * max(0f, 1f - i / 18f)
            out[i] = Easing.clamp(v, 0f, 1f) * 0.8f
        }
    }
}
