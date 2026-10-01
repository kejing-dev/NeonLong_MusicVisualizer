package com.technomancer.neonlong.logic.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Stereo placement for the fallback when the SDK can't spatialize: equal-power pan from the
 * source's azimuth, and a gentle distance falloff.
 */
object PanLaw {
    /**
     * Left and right gains for [azimuthRad] (0 = ahead, +π/2 = right); L² + R² = 1.
     * Writes into [out] as [left, right].
     */
    fun gains(azimuthRad: Float, out: FloatArray) {
        val pan = sin(azimuthRad.toDouble()).coerceIn(-1.0, 1.0)
        val angle = (pan + 1.0) * PI / 4.0
        out[0] = cos(angle).toFloat()
        out[1] = sin(angle).toFloat()
    }

    /** Distance gain 1 / max(1, d / ref)^0.5. */
    fun distanceGain(distance: Float, refDistance: Float = 1.5f): Float =
        (1.0 / max(1.0, (distance / refDistance).toDouble()).pow(0.5)).toFloat()

    /** Azimuth of a source at (dx, dz) relative to a listener facing −Z. */
    fun azimuth(dx: Float, dz: Float): Float {
        val len = sqrt(dx * dx + dz * dz)
        if (len < 1e-6f) return 0f
        return kotlin.math.atan2(dx, -dz)
    }
}
