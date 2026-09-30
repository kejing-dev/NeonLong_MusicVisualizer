package com.technomancer.neonlong.logic.math

import kotlin.math.pow
import kotlin.math.sqrt

/** Centripetal Catmull-Rom (alpha 0.5) evaluated on the segment p1 → p2. */
object CatmullRom {
    private const val ALPHA = 0.5f

    private fun knot(t: Float, a: Vec3, b: Vec3): Float {
        val dx = b.x - a.x; val dy = b.y - a.y; val dz = b.z - a.z
        val d = sqrt(dx * dx + dy * dy + dz * dz).pow(ALPHA)
        return t + if (d < 1e-6f) 1e-6f else d
    }

    /** out = the point at [u] ∈ [0, 1] between [p1] and [p2]. */
    fun eval(p0: Vec3, p1: Vec3, p2: Vec3, p3: Vec3, u: Float, out: Vec3): Vec3 {
        val t0 = 0f
        val t1 = knot(t0, p0, p1)
        val t2 = knot(t1, p1, p2)
        val t3 = knot(t2, p2, p3)
        val t = t1 + (t2 - t1) * u
        val ax = ((t1 - t) * p0.x + (t - t0) * p1.x) / (t1 - t0)
        val ay = ((t1 - t) * p0.y + (t - t0) * p1.y) / (t1 - t0)
        val az = ((t1 - t) * p0.z + (t - t0) * p1.z) / (t1 - t0)
        val bx = ((t2 - t) * p1.x + (t - t1) * p2.x) / (t2 - t1)
        val by = ((t2 - t) * p1.y + (t - t1) * p2.y) / (t2 - t1)
        val bz = ((t2 - t) * p1.z + (t - t1) * p2.z) / (t2 - t1)
        val cx = ((t3 - t) * p2.x + (t - t2) * p3.x) / (t3 - t2)
        val cy = ((t3 - t) * p2.y + (t - t2) * p3.y) / (t3 - t2)
        val cz = ((t3 - t) * p2.z + (t - t2) * p3.z) / (t3 - t2)
        val dx = ((t2 - t) * ax + (t - t0) * bx) / (t2 - t0)
        val dy = ((t2 - t) * ay + (t - t0) * by) / (t2 - t0)
        val dz = ((t2 - t) * az + (t - t0) * bz) / (t2 - t0)
        val ex = ((t3 - t) * bx + (t - t1) * cx) / (t3 - t1)
        val ey = ((t3 - t) * by + (t - t1) * cy) / (t3 - t1)
        val ez = ((t3 - t) * bz + (t - t1) * cz) / (t3 - t1)
        return out.set(
            ((t2 - t) * dx + (t - t1) * ex) / (t2 - t1),
            ((t2 - t) * dy + (t - t1) * ey) / (t2 - t1),
            ((t2 - t) * dz + (t - t1) * ez) / (t2 - t1),
        )
    }
}
