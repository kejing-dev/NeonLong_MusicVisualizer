package com.technomancer.neonlong.tools.demosynth

import com.technomancer.neonlong.logic.math.Rng
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/**
 * The demo track's instruments. Each note is rendered straight into the mix buffer
 * (and the delay send for plucks) starting at its time in seconds.
 */
class Voices(private val sampleRate: Int, private val mix: FloatArray, private val send: FloatArray) {
    /** Shared noise source, consumed in note order so the render is deterministic. */
    private val noiseRng = Rng(1234)

    private fun noise(): Double = noiseRng.next() * 2.0 - 1.0

    // ---- building blocks -------------------------------------------------------------

    /**
     * Envelope at [tau] seconds after the note starts: from 0.0001 up to [peak] exponentially
     * over [a] seconds, then back down to 0.0001 over [d] seconds.
     */
    fun env(tau: Double, a: Double, peak: Double, d: Double): Double = when {
        tau < 0 -> 0.0
        tau < a -> FLOOR * (peak / FLOOR).pow(tau / a)
        tau < a + d -> peak * (FLOOR / peak).pow((tau - a) / d)
        else -> 0.0
    }

    /** Exponential ramp from [v0] to [v1] over [dur] seconds, holding [v1] afterwards. */
    private fun expRamp(tau: Double, v0: Double, v1: Double, dur: Double): Double =
        if (tau >= dur) v1 else v0 * (v1 / v0).pow(tau / dur)

    private fun tri(p: Double) = 1.0 - 4.0 * abs(p - 0.5)
    private fun saw(p: Double) = 2.0 * p - 1.0
    private fun square(p: Double) = if (p < 0.5) 1.0 else -1.0
    private fun wrap(p: Double) = p - floor(p)
    private fun cents(f: Double, c: Double) = f * 2.0.pow(c / 1200.0)

    private inline fun render(start: Double, durS: Double, toSend: Boolean = false, sample: (tau: Double) -> Double) {
        val i0 = (start * sampleRate).toInt()
        val n = (durS * sampleRate).toInt()
        for (j in 0 until n) {
            val i = i0 + j
            if (i < 0 || i >= mix.size) continue
            val v = sample(j.toDouble() / sampleRate).toFloat()
            mix[i] += v
            if (toSend) send[i] += v
        }
    }

    // ---- instruments ------------------------------------------------------------------

    fun kick(t: Double) {
        var phase = 0.0
        render(t, 0.003 + 0.42) { tau ->
            val f = expRamp(tau, 165.0, 42.0, 0.13)
            phase = wrap(phase + f / sampleRate)
            sin(2 * PI * phase) * env(tau, 0.003, 1.0, 0.42)
        }
    }

    fun snare(t: Double) {
        val bp = Biquad(sampleRate).set(Biquad.Type.BANDPASS, 1900.0, 0.7)
        var phase = 0.0
        render(t, 0.21) { tau ->
            phase = wrap(phase + 185.0 / sampleRate)
            bp.process(noise()) * env(tau, 0.002, 0.5, 0.2) + tri(phase) * env(tau, 0.002, 0.3, 0.1)
        }
    }

    fun hat(t: Double, open: Boolean) {
        val hp = Biquad(sampleRate).set(Biquad.Type.HIGHPASS, 7500.0)
        val (peak, d) = if (open) 0.16 to 0.22 else 0.09 to 0.045
        render(t, 0.001 + d) { tau -> hp.process(noise()) * env(tau, 0.001, peak, d) }
    }

    fun bass(t: Double, midi: Int, dur: Double) {
        val f = mtof(midi)
        val lp = Biquad(sampleRate)
        var phase = 0.0
        render(t, 0.005 + dur) { tau ->
            phase = wrap(phase + f / sampleRate)
            lp.set(Biquad.Type.LOWPASS, expRamp(tau, 1500.0, 180.0, dur), 6.0)
            lp.process(saw(phase)) * env(tau, 0.005, 0.3, dur)
        }
    }

    fun pluck(t: Double, midi: Int, level: Double = 0.2) {
        val f = mtof(midi)
        val lp = Biquad(sampleRate)
        var p1 = 0.0
        var p2 = 0.0
        render(t, 0.002 + 0.9, toSend = true) { tau ->
            val pitch = expRamp(tau, 1.03 * f, f, 0.04)
            p1 = wrap(p1 + pitch / sampleRate)
            p2 = wrap(p2 + cents(pitch, 7.0) / sampleRate)
            lp.set(Biquad.Type.LOWPASS, expRamp(tau, 3600.0, 700.0, 0.5))
            lp.process(tri(p1) + 0.25 * square(p2)) * env(tau, 0.002, level, 0.9)
        }
    }

    fun pad(t: Double, notes: IntArray, dur: Double) {
        for (m in notes) {
            val f = mtof(m)
            val lp = Biquad(sampleRate).set(Biquad.Type.LOWPASS, 950.0)
            var pa = 0.0
            var pb = 0.0
            render(t, dur + 0.5) { tau ->
                pa = wrap(pa + cents(f, -8.0) / sampleRate)
                pb = wrap(pb + cents(f, 8.0) / sampleRate)
                val gain = when {
                    tau < 0.7 -> 0.05 * tau / 0.7
                    tau < dur - 0.2 -> 0.05
                    else -> 0.05 * (1.0 - (tau - (dur - 0.2)) / 0.7).coerceAtLeast(0.0)
                }
                lp.process(saw(pa) + saw(pb)) * gain
            }
        }
    }

    fun gong(t: Double) {
        for (k in GONG_RATIOS.indices) {
            val f0 = 82.0 * GONG_RATIOS[k]
            val peak = 0.22 / (k + 1)
            val d = 4.5 - 0.5 * k
            var phase = 0.0
            render(t, 0.01 + d) { tau ->
                phase = wrap(phase + expRamp(tau, f0, f0 * 0.985, 4.0) / sampleRate)
                sin(2 * PI * phase) * env(tau, 0.01, peak, d)
            }
        }
        val bp = Biquad(sampleRate).set(Biquad.Type.BANDPASS, 600.0)
        render(t, 0.502) { tau -> bp.process(noise()) * env(tau, 0.002, 0.25, 0.5) }
    }

    /** Short sine blip for interface hover. */
    fun uiBlip(freq: Double, durS: Double, peak: Double) {
        var phase = 0.0
        render(0.0, durS) { tau ->
            phase = wrap(phase + freq / sampleRate)
            sin(2 * PI * phase) * peak * fade(tau, durS)
        }
    }

    /** Short rising sine chirp for interface select. */
    fun uiChirp(f0: Double, f1: Double, durS: Double, peak: Double) {
        var phase = 0.0
        render(0.0, durS) { tau ->
            phase = wrap(phase + (f0 + (f1 - f0) * tau / durS) / sampleRate)
            sin(2 * PI * phase) * peak * fade(tau, durS)
        }
    }

    /** 1 ms linear fade in and out so interface sounds don't click. */
    private fun fade(tau: Double, durS: Double): Double {
        val edge = 0.001
        return minOf(1.0, tau / edge, (durS - tau) / edge).coerceAtLeast(0.0)
    }

    companion object {
        private const val FLOOR = 0.0001
        private val GONG_RATIOS = doubleArrayOf(1.0, 1.483, 1.932, 2.546, 2.63, 3.76)

        fun mtof(m: Int): Double = 440.0 * 2.0.pow((m - 69) / 12.0)
    }
}
