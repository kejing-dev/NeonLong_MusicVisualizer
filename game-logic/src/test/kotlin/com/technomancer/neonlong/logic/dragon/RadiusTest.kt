package com.technomancer.neonlong.logic.dragon

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RadiusTest {
    @Test
    fun endpoints() {
        assertEquals(1.131f, BodyLayout.radiusAt(0f), 1e-4f)
        assertEquals(0.29f, BodyLayout.radiusAt(1f), 1e-4f)
    }

    @Test
    fun nonIncreasingAfterNeck() {
        var prev = BodyLayout.radiusAt(0.14f)
        var u = 0.14f
        while (u <= 1f) {
            val r = BodyLayout.radiusAt(u)
            assertTrue(r <= prev + 1e-6f, "radius grew at u=$u")
            prev = r
            u += 0.001f
        }
    }
}
