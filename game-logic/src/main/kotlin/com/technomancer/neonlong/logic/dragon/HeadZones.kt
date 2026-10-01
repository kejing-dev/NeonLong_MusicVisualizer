package com.technomancer.neonlong.logic.dragon

/**
 * The head is drawn as one mesh per color zone. Head block i glows with palAt(0.04 · (i % 6))
 * times its glow weight, so blocks that share both values share a zone. Zones 0–5 are the
 * weight-1 blocks; the neck collar and the two nostrils have their own weight and zone.
 */
object HeadZones {
    /** Palette position of each zone. */
    val U = floatArrayOf(0f, 0.04f, 0.08f, 0.12f, 0.16f, 0.2f, 0.16f, 0f, 0.04f)

    /** Glow weight of each zone. */
    val GLOW = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 0.7f, 1.4f, 1.4f)

    val COUNT = U.size

    /** Zone of head block [block] with glow weight [glow]. */
    fun of(block: Int, glow: Float): Int {
        val u = 0.04f * (block % 6)
        for (z in 0 until COUNT) if (kotlin.math.abs(U[z] - u) < 1e-4f && GLOW[z] == glow) return z
        error("no head zone for block $block with glow $glow")
    }

    /** Zone for the teeth and pupils (closest to the jaw's color). */
    const val DETAIL = 2
}
