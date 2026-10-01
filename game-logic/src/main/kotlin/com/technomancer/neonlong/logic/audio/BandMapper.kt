package com.technomancer.neonlong.logic.audio

import com.technomancer.neonlong.logic.config.DragonConfig
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Groups FFT bins into [DragonConfig.BANDS] log-spaced bands from 32 Hz to 16 kHz.
 * Band i covers f0 = 32·500^(i/81) to f1 = 32·500^((i+1)/81); its value is the loudest bin.
 */
class BandMapper(sampleRate: Int, fftSize: Int) {
    val count = DragonConfig.BANDS
    val lo = IntArray(count)
    val hi = IntArray(count)

    init {
        val binHz = sampleRate.toDouble() / fftSize
        val maxBin = fftSize / 2
        for (i in 0 until count) {
            val f0 = 32.0 * 500.0.pow(i.toDouble() / count)
            val f1 = 32.0 * 500.0.pow((i + 1).toDouble() / count)
            val l = min(maxBin - 1, floor(f0 / binHz).toInt())
            lo[i] = l
            hi[i] = min(maxBin, max(l + 1, ceil(f1 / binHz).toInt()))
        }
    }

    /** out[i] = max of [bins] over band i's bin range. */
    fun map(bins: FloatArray, out: FloatArray) {
        for (i in 0 until count) {
            var m = 0f
            for (k in lo[i] until hi[i]) if (bins[k] > m) m = bins[k]
            out[i] = m
        }
    }
}
