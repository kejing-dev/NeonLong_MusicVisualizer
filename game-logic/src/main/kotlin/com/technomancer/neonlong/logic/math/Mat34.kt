package com.technomancer.neonlong.logic.math

/** Row-major 3×4 affine transforms stored in a FloatArray(12). */
object Mat34 {
    /** out = translate(t) · rotate(q) · scale(sx, sy, sz). */
    fun compose(q: Quat, sx: Float, sy: Float, sz: Float, t: Vec3, out: FloatArray) {
        val x = q.x; val y = q.y; val z = q.z; val w = q.w
        val xx = x * x; val yy = y * y; val zz = z * z
        val xy = x * y; val xz = x * z; val yz = y * z
        val wx = w * x; val wy = w * y; val wz = w * z
        out[0] = (1f - 2f * (yy + zz)) * sx; out[1] = 2f * (xy - wz) * sy; out[2] = 2f * (xz + wy) * sz; out[3] = t.x
        out[4] = 2f * (xy + wz) * sx; out[5] = (1f - 2f * (xx + zz)) * sy; out[6] = 2f * (yz - wx) * sz; out[7] = t.y
        out[8] = 2f * (xz - wy) * sx; out[9] = 2f * (yz + wx) * sy; out[10] = (1f - 2f * (xx + yy)) * sz; out[11] = t.z
    }

    /** out = m · p, treating p as a point. */
    fun transformPoint(m: FloatArray, p: Vec3, out: Vec3): Vec3 = out.set(
        m[0] * p.x + m[1] * p.y + m[2] * p.z + m[3],
        m[4] * p.x + m[5] * p.y + m[6] * p.z + m[7],
        m[8] * p.x + m[9] * p.y + m[10] * p.z + m[11],
    )
}
