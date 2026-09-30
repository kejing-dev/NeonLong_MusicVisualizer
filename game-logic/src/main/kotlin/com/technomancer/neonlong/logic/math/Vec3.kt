package com.technomancer.neonlong.logic.math

import kotlin.math.sqrt

/** Mutable 3D vector. Operations write in place so per-frame code never allocates. */
class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(x: Float, y: Float, z: Float): Vec3 {
        this.x = x; this.y = y; this.z = z
        return this
    }

    fun set(o: Vec3): Vec3 = set(o.x, o.y, o.z)

    fun add(o: Vec3): Vec3 = set(x + o.x, y + o.y, z + o.z)

    fun sub(o: Vec3): Vec3 = set(x - o.x, y - o.y, z - o.z)

    fun scale(s: Float): Vec3 = set(x * s, y * s, z * s)

    /** this += o · s */
    fun addScaled(o: Vec3, s: Float): Vec3 = set(x + o.x * s, y + o.y * s, z + o.z * s)

    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalize(): Vec3 {
        val l = length()
        return if (l > 1e-12f) scale(1f / l) else this
    }

    /** this = a × b (a and b may alias this). */
    fun cross(a: Vec3, b: Vec3): Vec3 =
        set(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

    /** this = a + (b − a) · t */
    fun lerp(a: Vec3, b: Vec3, t: Float): Vec3 =
        set(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t)

    override fun toString(): String = "($x, $y, $z)"
}
