package com.technomancer.neonlong.logic.audio

import com.technomancer.neonlong.logic.config.AudioConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Turns the latest [AudioConfig.fftSize] samples into [bins] normalized magnitudes in [0, 1]:
 * Blackman window → FFT → |X|/N → per-bin smoothing → dB → map [minDb, maxDb] to [0, 1].
 */
class SpectrumAnalyzer(private val c: AudioConfig = AudioConfig()) {
    val n = c.fftSize
    val bins = n / 2
    private val fft = Fft(n)
    private val window = FloatArray(n) { i ->
        val x = 2.0 * PI * i / n
        (0.42 - 0.5 * cos(x) + 0.08 * cos(2 * x)).toFloat()
    }
    private val re = FloatArray(n)
    private val im = FloatArray(n)
    private val smoothed = FloatArray(bins)

    /** Normalized output, one value per bin, updated by [analyze]. */
    val normalized = FloatArray(bins)

    /**
     * Analyzes the [n] samples of [src] ending just before index [end] (exclusive), wrapping
     * around the start of [src] when the window runs past it (for looping sources).
     */
    fun analyze(src: FloatArray, end: Int) {
        val len = src.size
        var j = end - n
        while (j < 0) j += len
        for (i in 0 until n) {
            re[i] = src[j] * window[i]
            im[i] = 0f
            j++
            if (j == len) j = 0
        }
        finish()
    }

    /** Analyzes exactly the [n] samples in [window] order already copied into [samples]. */
    fun analyzeWindow(samples: FloatArray) {
        for (i in 0 until n) {
            re[i] = samples[i] * window[i]
            im[i] = 0f
        }
        finish()
    }

    private fun finish() {
        fft.forward(re, im)
        val tau = c.smoothing
        val range = c.maxDb - c.minDb
        for (k in 0 until bins) {
            val mag = sqrt(re[k] * re[k] + im[k] * im[k]) / n
            val s = tau * smoothed[k] + (1f - tau) * mag
            smoothed[k] = s
            val db = if (s > 1e-12f) 20f * log10(s) else -240f
            val v = (db - c.minDb) / range
            normalized[k] = if (v < 0f) 0f else if (v > 1f) 1f else v
        }
    }

    fun reset() {
        smoothed.fill(0f)
        normalized.fill(0f)
    }

    /** Raw smoothed magnitude of bin [k] (for tests). */
    fun smoothedMagnitude(k: Int): Float = smoothed[k]
}
