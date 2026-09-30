package com.technomancer.neonlong.logic.math

import kotlin.math.exp

/** Scalar helpers shared by every formula. */
object Easing {
    fun clamp(v: Float, a: Float, b: Float): Float = if (v < a) a else if (v > b) b else v

    /** Smoothstep from [a] to [b]: t²(3 − 2t) with t clamped to [0, 1]. */
    fun smooth(a: Float, b: Float, x: Float): Float {
        val t = clamp((x - a) / (b - a), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** exp(−((x − m) / s)²). */
    fun gauss(x: Float, m: Float, s: Float): Float {
        val d = (x - m) / s
        return exp(-d * d)
    }

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
