package com.technomancer.neonlong.tools.demosynth

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** RBJ-cookbook biquad filter (direct form I). Coefficients can change every sample. */
class Biquad(private val sampleRate: Int) {
    enum class Type { LOWPASS, HIGHPASS, BANDPASS }

    private var b0 = 1.0; private var b1 = 0.0; private var b2 = 0.0
    private var a1 = 0.0; private var a2 = 0.0
    private var x1 = 0.0; private var x2 = 0.0; private var y1 = 0.0; private var y2 = 0.0

    fun set(type: Type, freq: Double, q: Double = DEFAULT_Q): Biquad {
        val f = freq.coerceIn(10.0, sampleRate * 0.49)
        val w0 = 2.0 * PI * f / sampleRate
        val cw = cos(w0)
        val alpha = sin(w0) / (2.0 * q)
        val a0: Double
        when (type) {
            Type.LOWPASS -> {
                b0 = (1 - cw) / 2; b1 = 1 - cw; b2 = (1 - cw) / 2
                a0 = 1 + alpha; a1 = -2 * cw; a2 = 1 - alpha
            }
            Type.HIGHPASS -> {
                b0 = (1 + cw) / 2; b1 = -(1 + cw); b2 = (1 + cw) / 2
                a0 = 1 + alpha; a1 = -2 * cw; a2 = 1 - alpha
            }
            Type.BANDPASS -> {
                // Constant 0 dB peak gain.
                b0 = alpha; b1 = 0.0; b2 = -alpha
                a0 = 1 + alpha; a1 = -2 * cw; a2 = 1 - alpha
            }
        }
        b0 /= a0; b1 /= a0; b2 /= a0; a1 /= a0; a2 /= a0
        return this
    }

    fun process(x: Double): Double {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1; x1 = x; y2 = y1; y1 = y
        return y
    }

    companion object {
        val DEFAULT_Q = 1.0 / sqrt(2.0)
    }
}
