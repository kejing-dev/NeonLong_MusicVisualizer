package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.cos
import kotlin.math.sin

/** The closed flight loop the head follows, parameterized by s (radians). Model units, Y up. */
object FlightPath {
    /** out = (15·sin s, 2.2 + 2.4·sin(3s + 0.8), 9.5·sin 2s). */
    fun basePt(s: Float, out: Vec3): Vec3 =
        out.set(15f * sin(s), 2.2f + 2.4f * sin(3f * s + 0.8f), 9.5f * sin(2f * s))

    /** out = d basePt / ds (not normalized). Points in the direction of travel. */
    fun tangent(s: Float, out: Vec3): Vec3 =
        out.set(15f * cos(s), 7.2f * cos(3f * s + 0.8f), 19f * cos(2f * s))
}
