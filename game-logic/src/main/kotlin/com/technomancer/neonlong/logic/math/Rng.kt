package com.technomancer.neonlong.logic.math

/**
 * Small deterministic generator with 32-bit integer state. Used wherever randomness is needed
 * so generated assets and simulations are reproducible.
 */
class Rng(seed: Int) {
    private var state: Int = seed

    /** Next value in [0, 1). */
    fun next(): Double {
        state += 0x6D2B79F5
        var t = (state xor (state ushr 15)) * (1 or state)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return ((t xor (t ushr 14)).toLong() and 0xffffffffL) / 4294967296.0
    }

    /** Uniform in [a, b). */
    fun range(a: Float, b: Float): Float = (a + (b - a) * next()).toFloat()
}
