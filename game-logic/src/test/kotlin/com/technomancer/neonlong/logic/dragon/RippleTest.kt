package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RippleTest {
    private val config = DragonConfig()

    private fun peakNear(waves: Waves, around: Float): Float {
        var best = around
        var bestV = -1f
        var u = around - 0.1f
        while (u <= around + 0.1f) {
            val v = waves.waveAt(u)
            if (v > bestV) { bestV = v; best = u }
            u += 0.0005f
        }
        return best
    }

    @Test
    fun rippleTravelsBothWays() {
        val waves = Waves(config)
        val seg = 13
        val u0 = BodyLayout.segmentU(seg)
        assertTrue(waves.ripple(seg, u0))
        val dt = 1f / 72f
        var t = 0f
        repeat(22) { waves.update(dt); t += dt }
        val expected = config.rippleSpeed * t
        assertEquals(u0 + expected, peakNear(waves, u0 + expected), 0.003f)
        assertEquals(u0 - expected, peakNear(waves, u0 - expected), 0.003f)
        assertEquals(config.rippleStrength, waves.waveAt(u0 + expected), 0.02f)
    }

    @Test
    fun touchedSegmentGlowsAndFades() {
        val waves = Waves(config)
        waves.ripple(4, BodyLayout.segmentU(4))
        assertEquals(config.rippleFlash, waves.flashOf(4), 1e-6f)
        waves.update(0.25f)
        assertEquals(config.rippleFlash * kotlin.math.exp(-config.rippleFlashDecay * 0.25f), waves.flashOf(4), 1e-4f)
    }

    @Test
    fun perSegmentCooldown() {
        val waves = Waves(config)
        assertTrue(waves.ripple(7, BodyLayout.segmentU(7)))
        assertFalse(waves.ripple(7, BodyLayout.segmentU(7)), "second touch inside the cooldown")
        assertTrue(waves.ripple(8, BodyLayout.segmentU(8)), "other segments aren't blocked")
        waves.update(0.26f)
        assertTrue(waves.ripple(7, BodyLayout.segmentU(7)), "allowed again after 0.25 s")
    }

    @Test
    fun beatWaveMovesHeadToTailAndExpires() {
        val waves = Waves(config)
        waves.spawnBeat(1.0f)
        val dt = 1f / 72f
        var t = 0f
        repeat(36) { waves.update(dt); t += dt }
        val expected = config.waveSpeed * t
        assertEquals(expected, peakNear(waves, expected), 0.003f)
        waves.update(config.waveLifeS)
        assertEquals(0, waves.activeWaves)
    }
}
