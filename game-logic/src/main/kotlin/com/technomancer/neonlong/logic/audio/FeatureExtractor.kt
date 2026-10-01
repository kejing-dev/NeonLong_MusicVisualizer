package com.technomancer.neonlong.logic.audio

import com.technomancer.neonlong.logic.config.AudioConfig
import com.technomancer.neonlong.logic.config.DragonConfig
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Turns raw band levels into the smoothed values the dragon reads: each band gets a
 * gate (ignore below 0.2), a curve, a treble lift and the user's sensitivity, then a
 * fast-rise / slow-fall follower. Also averages bass, mid, high and overall volume.
 */
class FeatureExtractor(private val c: AudioConfig = AudioConfig()) {
    private val count = DragonConfig.BANDS

    /** Per-band targets before smoothing (the beat detector reads these). */
    val targets = FloatArray(count)

    /** Smoothed per-band values. */
    val bands = FloatArray(count)
    var bass = 0f; private set
    var mid = 0f; private set
    var high = 0f; private set
    var vol = 0f; private set

    var sensitivity = c.sensitivity

    fun update(raw: FloatArray, dt: Float) {
        val up = 1f - exp(-c.attackRate * dt)
        val down = 1f - exp(-c.releaseRate * dt)
        for (i in 0 until count) {
            val v = max(0f, (raw[i] - 0.2f) / 0.8f)
            val t = min(1.25f, v.pow(1.25f) * (0.95f + 0.85f * i / count) * sensitivity)
            targets[i] = t
            val cur = bands[i]
            bands[i] = cur + (t - cur) * (if (t > cur) up else down)
        }
        bass = mean(0, 9)
        mid = mean(28, 51)
        high = mean(55, 80)
        vol = mean(0, count - 1)
    }

    /** Sets bands directly (idle generator) and recomputes the averages. */
    fun setDirect(values: FloatArray) {
        for (i in 0 until count) {
            bands[i] = values[i]
            targets[i] = values[i]
        }
        bass = mean(0, 9); mid = mean(28, 51); high = mean(55, 80); vol = mean(0, count - 1)
    }

    fun reset() {
        targets.fill(0f)
        bands.fill(0f)
        bass = 0f; mid = 0f; high = 0f; vol = 0f
    }

    /** Mean of bands [a]..[b] inclusive. */
    private fun mean(a: Int, b: Int): Float {
        var s = 0f
        for (i in a..b) s += bands[i]
        return s / (b - a + 1)
    }

    /** Mean of unsmoothed targets [a]..[b] inclusive. */
    fun targetMean(a: Int, b: Int): Float {
        var s = 0f
        for (i in a..b) s += targets[i]
        return s / (b - a + 1)
    }
}
