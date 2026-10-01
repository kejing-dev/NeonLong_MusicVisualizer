package com.technomancer.neonlong.render

/**
 * Keeps glow colors displayable: very bright colors are scaled down as a whole, so they keep
 * their hue instead of clipping to white.
 */
object Glow {
    /** Brightest channel value sent to the renderer. */
    const val MAX_CHANNEL = 1.0f

    /** Factor that brings the brightest channel of (r, g, b) down to [MAX_CHANNEL] (1 if already below). */
    fun capScale(r: Float, g: Float, b: Float): Float {
        val m = maxOf(r, maxOf(g, b))
        return if (m > MAX_CHANNEL) MAX_CHANNEL / m else 1f
    }
}
