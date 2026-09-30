package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Easing
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The body's centerline: [steps] + 1 samples from the head (u = 0) to the tail (u = 1),
 * trailing the head along the flight path and undulating sideways and up.
 */
class Spine(val steps: Int, private val rollAmp: Float) {
    /** Sample positions, xyz interleaved. */
    val samples = FloatArray((steps + 1) * 3)

    private var time = 0f
    private val base = Vec3()
    private val tan = Vec3()
    private val side = Vec3()
    private val up = Vec3()
    private val worldUp = Vec3(0f, 1f, 0f)
    private val a = Vec3()
    private val b = Vec3()

    /**
     * Recomputes every sample for head parameter [sHead] at time [t].
     * [undulation] scales the side and up waves (Reduce motion uses 0.6).
     * [pull] ∈ [0, 1] draws the loop toward the user: z becomes z·(1 − pull) + pull·[frontZ],
     * which moves the loop's center forward while shrinking it so it stays in bounds.
     */
    fun update(
        sHead: Float,
        bodyLength: Float,
        t: Float,
        bass: Float,
        undulation: Float,
        pull: Float = 0f,
        frontZ: Float = 0f,
    ) {
        time = t
        for (k in 0..steps) {
            val u = k.toFloat() / steps
            val s = sHead - u * bodyLength
            FlightPath.basePt(s, base)
            FlightPath.tangent(s, tan).normalize()
            side.cross(worldUp, tan).normalize()
            up.cross(tan, side)
            val sideAmt = (0.45f + 0.7f * u) * (1f + 0.5f * bass) * sin(10f * u - 2.3f * t) * undulation
            val upAmt = 0.5f * (0.3f + u) * cos(7f * u - 1.6f * t) * undulation
            val i = k * 3
            samples[i] = base.x + side.x * sideAmt + up.x * upAmt
            samples[i + 1] = base.y + side.y * sideAmt + up.y * upAmt
            val z = base.z + side.z * sideAmt + up.z * upAmt
            samples[i + 2] = z * (1f - pull) + pull * frontZ
        }
    }

    fun sample(k: Int, out: Vec3): Vec3 {
        val i = k * 3
        return out.set(samples[i], samples[i + 1], samples[i + 2])
    }

    /** Frame at [u] ∈ [0, 1] using the time of the last [update], including the body roll. */
    fun frameAt(u: Float, out: Basis): Basis {
        val x = Easing.clamp(u, 0f, 1f) * steps
        val k = min(steps - 1, floor(x).toInt())
        val f = x - k
        sample(k, a)
        sample(k + 1, b)
        out.p.lerp(a, b, f)
        sample(max(0, k - 1), a)
        sample(min(steps, k + 2), b)
        out.f.set(a).sub(b).normalize()
        out.s.cross(worldUp, out.f).normalize()
        out.u.cross(out.f, out.s)
        val roll = rollAmp * sin(6f * u - 1.3f * time)
        val c = cos(roll)
        val sn = sin(roll)
        // S' = S·cos a + U·sin a ; U' = U·cos a − S·sin a
        val sx = out.s.x * c + out.u.x * sn
        val sy = out.s.y * c + out.u.y * sn
        val sz = out.s.z * c + out.u.z * sn
        out.u.set(out.u.x * c - out.s.x * sn, out.u.y * c - out.s.y * sn, out.u.z * c - out.s.z * sn)
        out.s.set(sx, sy, sz)
        return out
    }
}
