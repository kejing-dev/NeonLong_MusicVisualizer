package com.technomancer.neonlong.logic.view

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class TurntableMathTest {
    private val quarter = (PI / 2).toFloat()

    @Test
    fun ninetyDegreesOfArcGivesNinetyDegreesOfYaw() {
        val r = 0.4f
        // From +X, a quarter turn counter-clockwise seen from above ends at −Z.
        val yaw = TurntableMath.yawFromArc(r, 0f, r * cos(quarter), -r * sin(quarter), 1.0f)
        assertEquals(quarter, yaw, 1e-5f)
    }

    @Test
    fun arcAcrossTheWrapIsShortWay() {
        val a = TurntableMath.yawFromArc(-1f, 0.01f, -1f, -0.01f, 1f)
        assertEquals(0.02f, kotlin.math.abs(a), 1e-3f)
    }

    @Test
    fun gainScales() {
        val yaw = TurntableMath.yawFromArc(1f, 0f, 0f, -1f, 0.5f)
        assertEquals(quarter * 0.5f, yaw, 1e-5f)
    }

    @Test
    fun slideAlongFrontMatchesArc() {
        val r = 0.54f
        assertEquals(quarter, TurntableMath.yawFromSlide(quarter * r, r, 1.0f), 1e-5f)
    }

    @Test
    fun hapticTicksEveryFifteenDegrees() {
        val deg = (PI / 180).toFloat()
        assertEquals(6, TurntableMath.ticksCrossed(0.5f * deg, 90.5f * deg, 15f))
        assertEquals(0, TurntableMath.ticksCrossed(1f * deg, 14f * deg, 15f))
    }
}
