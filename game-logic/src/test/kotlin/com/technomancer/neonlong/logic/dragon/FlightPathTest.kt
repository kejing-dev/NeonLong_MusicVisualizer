package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.PI

class FlightPathTest {
    @Test
    fun basePtAtZero() {
        val p = FlightPath.basePt(0f, Vec3())
        assertEquals(0f, p.x, 1e-6f)
        assertEquals(3.9216546f, p.y, 1e-6f)
        assertEquals(0f, p.z, 1e-6f)
    }

    @Test
    fun basePtAtHalfPi() {
        val p = FlightPath.basePt((PI / 2).toFloat(), Vec3())
        assertEquals(15f, p.x, 1e-6f)
        assertEquals(0.5279039f, p.y, 1e-6f)
        assertEquals(0f, p.z, 1e-6f)
    }

    @Test
    fun tangentMatchesFiniteDifference() {
        val a = Vec3(); val b = Vec3(); val t = Vec3()
        val h = 1e-3f
        for (i in 0 until 50) {
            val s = i * 0.13f
            FlightPath.basePt(s - h, a)
            FlightPath.basePt(s + h, b)
            FlightPath.tangent(s, t)
            assertEquals(t.x, (b.x - a.x) / (2 * h), 0.02f)
            assertEquals(t.y, (b.y - a.y) / (2 * h), 0.02f)
            assertEquals(t.z, (b.z - a.z) / (2 * h), 0.02f)
        }
    }
}
