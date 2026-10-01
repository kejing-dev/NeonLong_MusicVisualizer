package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.PaletteConfig
import com.technomancer.neonlong.logic.math.Color3

/**
 * The active color scheme. [palAt] blends stop0→stop1 over [0, 0.5] and stop1→stop2 over
 * [0.5, 1]. Switching palettes crossfades every color over [PaletteConfig.fadeS].
 */
class Palette(private val config: PaletteConfig = PaletteConfig()) {
    private class Stops(val s0: Color3, val s1: Color3, val s2: Color3, val accent: Color3)

    private val defs = config.palettes.map {
        Stops(Color3.fromHex(it.stop0), Color3.fromHex(it.stop1), Color3.fromHex(it.stop2), Color3.fromHex(it.accent))
    }

    val count: Int get() = defs.size
    var index = 0
        private set
    private var from = 0
    private var fade = 1f

    /** Current (blended) colors. */
    val stop0 = Color3(); val stop1 = Color3(); val stop2 = Color3(); val accent = Color3()

    init {
        blend()
    }

    fun nameOf(i: Int): String = config.palettes[i].name

    /** Switches to palette [i], crossfading from the current colors. */
    fun select(i: Int) {
        if (i == index && fade >= 1f) return
        from = index
        index = i.coerceIn(0, defs.size - 1)
        fade = 0f
    }

    fun update(dt: Float) {
        if (fade < 1f) {
            fade = if (config.fadeS <= 0f) 1f else (fade + dt / config.fadeS).coerceAtMost(1f)
            blend()
        }
    }

    /** out = the gradient color at [u] (clamped to [0, 1]). */
    fun palAt(u: Float, out: Color3): Color3 {
        val x = if (u < 0f) 0f else if (u > 1f) 1f else u
        return if (x <= 0.5f) out.lerp(stop0, stop1, x * 2f) else out.lerp(stop1, stop2, (x - 0.5f) * 2f)
    }

    private fun blend() {
        val a = defs[from]
        val b = defs[index]
        stop0.lerp(a.s0, b.s0, fade)
        stop1.lerp(a.s1, b.s1, fade)
        stop2.lerp(a.s2, b.s2, fade)
        accent.lerp(a.accent, b.accent, fade)
    }
}
