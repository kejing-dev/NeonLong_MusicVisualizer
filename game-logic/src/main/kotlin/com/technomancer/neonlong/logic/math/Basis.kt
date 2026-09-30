package com.technomancer.neonlong.logic.math

/**
 * A spine frame: position [p] and orthonormal axes side [s], up [u] and forward [f]
 * (forward points toward the head). Used as rotation columns (S, U, F).
 */
class Basis {
    val p = Vec3()
    val s = Vec3(1f, 0f, 0f)
    val u = Vec3(0f, 1f, 0f)
    val f = Vec3(0f, 0f, 1f)

    fun set(o: Basis): Basis {
        p.set(o.p); s.set(o.s); u.set(o.u); f.set(o.f)
        return this
    }
}
