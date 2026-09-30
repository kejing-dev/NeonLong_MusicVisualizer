package com.technomancer.neonlong.logic.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Mutable unit quaternion (x, y, z, w). */
class Quat(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    fun set(x: Float, y: Float, z: Float, w: Float): Quat {
        this.x = x; this.y = y; this.z = z; this.w = w
        return this
    }

    fun identity(): Quat = set(0f, 0f, 0f, 1f)

    /** Rotation of [angle] radians about the unit [axis]. */
    fun setAxisAngle(axis: Vec3, angle: Float): Quat {
        val h = angle * 0.5f
        val s = sin(h)
        return set(axis.x * s, axis.y * s, axis.z * s, cos(h))
    }

    /** Rotation whose matrix columns are the orthonormal axes [c0], [c1], [c2]. */
    fun setFromColumns(c0: Vec3, c1: Vec3, c2: Vec3): Quat {
        val m00 = c0.x; val m10 = c0.y; val m20 = c0.z
        val m01 = c1.x; val m11 = c1.y; val m21 = c1.z
        val m02 = c2.x; val m12 = c2.y; val m22 = c2.z
        val trace = m00 + m11 + m22
        if (trace > 0f) {
            val s = sqrt(trace + 1f) * 2f
            set((m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s, 0.25f * s)
        } else if (m00 > m11 && m00 > m22) {
            val s = sqrt(1f + m00 - m11 - m22) * 2f
            set(0.25f * s, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s)
        } else if (m11 > m22) {
            val s = sqrt(1f + m11 - m00 - m22) * 2f
            set((m01 + m10) / s, 0.25f * s, (m12 + m21) / s, (m02 - m20) / s)
        } else {
            val s = sqrt(1f + m22 - m00 - m11) * 2f
            set((m02 + m20) / s, (m12 + m21) / s, 0.25f * s, (m10 - m01) / s)
        }
        return normalize()
    }

    /** this = a · b (a and b may alias this). */
    fun mul(a: Quat, b: Quat): Quat = set(
        a.w * b.x + a.x * b.w + a.y * b.z - a.z * b.y,
        a.w * b.y - a.x * b.z + a.y * b.w + a.z * b.x,
        a.w * b.z + a.x * b.y - a.y * b.x + a.z * b.w,
        a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z,
    )

    fun normalize(): Quat {
        val l = sqrt(x * x + y * y + z * z + w * w)
        return if (l > 1e-12f) set(x / l, y / l, z / l, w / l) else identity()
    }

    /** out = v rotated by this quaternion. */
    fun rotate(v: Vec3, out: Vec3): Vec3 {
        val tx = 2f * (y * v.z - z * v.y)
        val ty = 2f * (z * v.x - x * v.z)
        val tz = 2f * (x * v.y - y * v.x)
        return out.set(
            v.x + w * tx + (y * tz - z * ty),
            v.y + w * ty + (z * tx - x * tz),
            v.z + w * tz + (x * ty - y * tx),
        )
    }
}
