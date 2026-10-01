package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Chrome pistons bridging each gap between segments (two per gap, a sleeve over a rod), and
 * two glowing cables running along the body's sides. Both are hidden in small volumes.
 */
class Pistons {
    private val a = Vec3()
    private val b = Vec3()
    private val f = Basis()
    private val from = Color3()
    private val to = Color3()

    fun write(out: PoseBuffer, place: Place, frames: Array<Basis>, radius: FloatArray, depth: FloatArray, visible: Boolean) {
        for (i in 0 until DragonConfig.SEGMENTS - 1) {
            val fa = frames[i]; val fb = frames[i + 1]
            val ra = radius[i]; val rb = radius[i + 1]
            for (s in 0..1) {
                val th = (if (s == 1) 1f else -1f) * 1.2f
                val st = sin(th) * 0.86f
                val ct = cos(th) * 0.86f
                a.set(fa.p).addScaled(fa.f, -0.42f * depth[i]).addScaled(fa.s, st * ra).addScaled(fa.u, ct * ra)
                b.set(fb.p).addScaled(fb.f, 0.42f * depth[i + 1]).addScaled(fb.s, st * rb).addScaled(fb.u, ct * rb)
                val n = i * 2 + s
                val len = place.along(out, PartFamily.PISTON_ROD, n, a, b, 0.03f * ra, 0.03f * ra)
                // Sleeve: same start and direction, 55% of the length, thicker.
                place.along(out, PartFamily.PISTON_SLEEVE, n, a, b, 0.065f * ra, 0.065f * ra)
                val t = out.transforms(PartFamily.PISTON_SLEEVE)
                t[n * PoseBuffer.TRANSFORM_STRIDE + 8] = 0.55f * len
                out.setAppearance(PartFamily.PISTON_ROD, n, 0f, 0f, 0f, visible)
                out.setAppearance(PartFamily.PISTON_SLEEVE, n, 0f, 0f, 0f, visible)
            }
        }
    }

    /** Fills the two side cables, glowing with bass and beats. */
    fun writeCables(fibers: Fibers, spine: Spine, palette: Palette, bass: Float, pulse: Float, visible: Boolean, gain: Float) {
        from.set(palette.stop2)
        to.set(palette.stop0)
        val g = (0.6f + 1.5f * bass + pulse) * gain
        for (c in 0..1) {
            val side = if (c == 0) -1f else 1f
            val fib = fibers.cables[c]
            fib.visible = visible
            for (k in 0 until fib.points) {
                val a01 = k / (fib.points - 1f)
                val u = 0.03f + 0.92f * a01
                spine.frameAt(u, f)
                val r = BodyLayout.radiusAt(u)
                fib.xyz[k * 3] = f.p.x + f.s.x * side * r - f.u.x * 0.02f * r
                fib.xyz[k * 3 + 1] = f.p.y + f.s.y * side * r - f.u.y * 0.02f * r
                fib.xyz[k * 3 + 2] = f.p.z + f.s.z * side * r - f.u.z * 0.02f * r
                fib.radius[k] = 0.07f + (0.05f - 0.07f) * a01
                fib.rgb[k * 3] = (from.r + (to.r - from.r) * a01) * g
                fib.rgb[k * 3 + 1] = (from.g + (to.g - from.g) * a01) * g
                fib.rgb[k * 3 + 2] = (from.b + (to.b - from.b) * a01) * g
            }
        }
    }
}
