package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** A fan of 18 flame ribbons at the tail; ribbon i reads band 62 + i and grows with it. */
class TailFan {
    private val f = Basis()
    private val d = Vec3()
    private val from = Color3()
    private val to = Color3()

    fun write(fibers: Fibers, spine: Spine, palette: Palette, bands: FloatArray, t: Float, high: Float, gain: Float) {
        spine.frameAt(1f, f)
        palette.palAt(0.85f, from)
        val g = (0.7f + 1.6f * high) * gain
        for (i in 0 until RIBBONS) {
            val fib = fibers.tail[i]
            val ang = -1.4f + 2.8f * i / 17f
            d.set(f.f).scale(-1f).addScaled(f.u, 0.75f * cos(ang)).addScaled(f.s, 0.75f * sin(ang)).normalize()
            val v = bands[62 + i]
            val len = (2.2f + 0.6f * (i % 3)) * (1f - 0.18f * abs(ang)) * (0.8f + 1.4f * v + 0.5f * high)
            if (i % 2 == 1) to.set(palette.accent).scale(1.6f) else to.set(palette.stop2).scale(1.8f)
            val r0 = 0.11f - 0.02f * (i % 3)
            for (k in 0 until fib.points) {
                val a = k / (fib.points - 1f)
                val sw = sin(3.4f * t + i + 5f * a) * 0.3f * a
                val uw = cos(2.8f * t + 1.3f * i + 4f * a) * 0.25f * a
                val back = -0.8f * a * a
                fib.xyz[k * 3] = f.p.x + d.x * len * a + f.f.x * back + f.s.x * sw + f.u.x * uw
                fib.xyz[k * 3 + 1] = f.p.y + d.y * len * a + f.f.y * back + f.s.y * sw + f.u.y * uw
                fib.xyz[k * 3 + 2] = f.p.z + d.z * len * a + f.f.z * back + f.s.z * sw + f.u.z * uw
                fib.radius[k] = r0 + (0.004f - r0) * a
                fib.rgb[k * 3] = (from.r * 0.8f + (to.r - from.r * 0.8f) * a) * g
                fib.rgb[k * 3 + 1] = (from.g * 0.8f + (to.g - from.g * 0.8f) * a) * g
                fib.rgb[k * 3 + 2] = (from.b * 0.8f + (to.b - from.b * 0.8f) * a) * g
            }
        }
    }

    companion object {
        const val RIBBONS = 18
    }
}
