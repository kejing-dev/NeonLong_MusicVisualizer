package com.technomancer.neonlong.tools.demosynth

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow

/** Feed-forward compressor with a soft knee, applied in place. */
class Compressor(
    private val sampleRate: Int,
    private val thresholdDb: Double = -14.0,
    private val ratio: Double = 4.0,
    private val kneeDb: Double = 30.0,
    attackS: Double = 0.003,
    releaseS: Double = 0.25,
) {
    private val attack = exp(-1.0 / (attackS * sampleRate))
    private val release = exp(-1.0 / (releaseS * sampleRate))

    fun process(buf: FloatArray) {
        var reductionDb = 0.0
        for (i in buf.indices) {
            val x = buf[i].toDouble()
            val levelDb = 20.0 * log10(abs(x) + 1e-9)
            val target = gainReduction(levelDb)
            // Reduction deepens at the attack rate and recovers at the release rate.
            val coeff = if (target < reductionDb) attack else release
            reductionDb = coeff * reductionDb + (1 - coeff) * target
            buf[i] = (x * 10.0.pow(reductionDb / 20.0)).toFloat()
        }
    }

    /** Static curve: gain change in dB (≤ 0) for an input level. */
    private fun gainReduction(x: Double): Double {
        val over = x - thresholdDb
        val y = when {
            2 * over < -kneeDb -> x
            2 * abs(over) <= kneeDb -> x + (1 / ratio - 1) * (over + kneeDb / 2).pow(2) / (2 * kneeDb)
            else -> thresholdDb + over / ratio
        }
        return y - x
    }
}
