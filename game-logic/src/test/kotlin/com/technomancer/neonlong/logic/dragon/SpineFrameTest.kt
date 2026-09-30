package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Rng
import com.technomancer.neonlong.logic.math.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpineFrameTest {
    private val config = DragonConfig()

    @Test
    fun framesAreOrthonormal() {
        val spine = Spine(config.spineSamples, config.rollAmp)
        val rng = Rng(42)
        val frame = Basis()
        repeat(1000) {
            val t = rng.range(0f, 200f)
            val sHead = rng.range(0f, 50f)
            spine.update(sHead, config.bodyLength, t, rng.range(0f, 1.25f), 1f)
            spine.frameAt(rng.next().toFloat(), frame)
            assertEquals(1f, frame.s.length(), 1e-4f)
            assertEquals(1f, frame.u.length(), 1e-4f)
            assertEquals(1f, frame.f.length(), 1e-4f)
            assertEquals(0f, frame.s.dot(frame.u), 1e-4f)
            assertEquals(0f, frame.s.dot(frame.f), 1e-4f)
            assertEquals(0f, frame.u.dot(frame.f), 1e-4f)
        }
    }

    @Test
    fun forwardAtHeadPointsTowardHead() {
        val spine = Spine(config.spineSamples, config.rollAmp)
        spine.update(1.3f, config.bodyLength, 7f, 0.4f, 1f)
        val frame = spine.frameAt(0f, Basis())
        val s0 = spine.sample(0, Vec3())
        val s1 = spine.sample(1, Vec3())
        val dir = s0.sub(s1).normalize()
        assertTrue(frame.f.dot(dir) > 0.99f, "F·(s0 − s1) = ${frame.f.dot(dir)}")
    }
}
