package com.technomancer.neonlong.render

/**
 * Keeps glow colors bright but displayable: every glow is lifted by [boost], and very bright
 * colors are scaled down as a whole, so they keep their hue instead of clipping to white.
 */
object Glow {
    /** Brightest channel value sent to the renderer. */
    const val MAX_CHANNEL = 1.0f

    /**
     * Overall brightness lift for every glow, set from the scene config when the dragon is
     * built. It makes up for the missing bloom; colors that would clip are capped as a whole.
     */
    @Volatile
    var boost = 1f

    /**
     * Factor to multiply (r, g, b) by: [boost], or less if that would push the brightest
     * channel past [MAX_CHANNEL].
     */
    fun capScale(r: Float, g: Float, b: Float): Float {
        val m = maxOf(r, maxOf(g, b))
        return if (m * boost > MAX_CHANNEL) MAX_CHANNEL / m else boost
    }
}
