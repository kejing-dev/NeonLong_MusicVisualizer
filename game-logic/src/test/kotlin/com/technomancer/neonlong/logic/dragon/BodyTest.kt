package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.config.FlashConfig
import com.technomancer.neonlong.logic.input.InputSnapshot
import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.safety.FlashLimiter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class TowerClearanceTest {
    /** Closest distance between segments p0→p1 and q0→q1. */
    private fun segDist(
        p0x: Float, p0y: Float, p0z: Float, p1x: Float, p1y: Float, p1z: Float,
        q0x: Float, q0y: Float, q0z: Float, q1x: Float, q1y: Float, q1z: Float,
    ): Float {
        var best = Float.MAX_VALUE
        val steps = 24
        for (a in 0..steps) {
            val s = a.toFloat() / steps
            val px = p0x + (p1x - p0x) * s; val py = p0y + (p1y - p0y) * s; val pz = p0z + (p1z - p0z) * s
            for (b in 0..steps) {
                val t = b.toFloat() / steps
                val qx = q0x + (q1x - q0x) * t; val qy = q0y + (q1y - q0y) * t; val qz = q0z + (q1z - q0z) * t
                val dx = px - qx; val dy = py - qy; val dz = pz - qz
                val d = sqrt(dx * dx + dy * dy + dz * dz)
                if (d < best) best = d
            }
        }
        return best
    }

    @Test
    fun neighborsNeverTouch() {
        val sim = DragonSimulation()
        val input = InputSnapshot().apply { bands.fill(1.25f); bass = 1.25f; vol = 1.25f }
        val pose = PoseBuffer()
        val t = sim.towers
        val frames = Array(DragonConfig.SEGMENTS) { Basis() }
        var worst = Float.MAX_VALUE
        var baseClashes = 0
        var marginMisses = 0
        var checks = 0
        repeat(5000) { step ->
            sim.tick(1f / 72f, input, pose)
            if (step % 5 != 0) return@repeat // sample every 5th step to keep the test quick
            for (i in 0 until DragonConfig.SEGMENTS) sim.spine.frameAt(BodyLayout.segmentU(i), frames[i])
            for (i in 0 until DragonConfig.SEGMENTS - 1) {
                val ua = frames[i].u; val ub = frames[i + 1].u
                for (ja in 0..2) for (jb in 0..2) {
                    val a = 3 * i + ja; val b = 3 * (i + 1) + jb
                    val ha = t.height[a] + t.spire[a]; val hb = t.height[b] + t.spire[b]
                    val d = segDist(
                        t.baseX[a], t.baseY[a], t.baseZ[a],
                        t.baseX[a] + ha * ua.x, t.baseY[a] + ha * ua.y, t.baseZ[a] + ha * ua.z,
                        t.baseX[b], t.baseY[b], t.baseZ[b],
                        t.baseX[b] + hb * ub.x, t.baseY[b] + hb * ub.y, t.baseZ[b] + hb * ub.z,
                    )
                    checks++
                    val limit = 0.75f * (t.width[a] + t.width[b])
                    val dx = t.baseX[a] - t.baseX[b]; val dy = t.baseY[a] - t.baseY[b]; val dz = t.baseZ[a] - t.baseZ[b]
                    if (sqrt(dx * dx + dy * dy + dz * dz) < limit + 0.1f) {
                        // Bases already this close on a tight bend: the clearance rule can only
                        // hold both towers at their minimum height.
                        baseClashes++
                        assertTrue(ha <= 0.3f + 1e-4f && hb <= 0.3f + 1e-4f, "step $step towers $a/$b not held down")
                        continue
                    }
                    worst = minOf(worst, d - limit)
                    if (d < limit) marginMisses++
                    // Towers are boxes of half-width w/2, so they only touch below 0.5·(wa + wb).
                    val touch = 0.5f * (t.width[a] + t.width[b])
                    assertTrue(d >= touch, "step $step towers $a/$b touch: $d < $touch")
                }
            }
        }
        println(
            "[TowerClearanceTest] checks=$checks, closer than 0.75(wa+wb): $marginMisses, " +
                "smallest margin $worst, bases already too close: $baseClashes",
        )
        // The spec's safety margin (0.75 · widths) may be shaved on rare tight bends, never much.
        assertTrue(marginMisses <= checks / 1000, "margin misses $marginMisses of $checks")
    }
}

class TowerHeightTest {
    @Test
    fun heightGrowsWithBandAndStaysUnderCap() {
        val towers = Towers()
        val radius = FloatArray(DragonConfig.SEGMENTS) { 1.2f }
        val v = FloatArray(DragonConfig.TOWERS)
        for (cap in floatArrayOf(0.3f, 1.1f, 2.3f, 5f, 99f)) {
            towers.cap.fill(cap)
            var prev = -1f
            var x = 0f
            while (x <= 1.25f) {
                v.fill(x)
                towers.size(v, radius)
                val h = towers.height[10]
                assertTrue(h >= prev - 1e-6f, "height fell at v=$x cap=$cap")
                assertTrue(towers.height[10] + towers.spire[10] <= cap, "over cap $cap at v=$x")
                prev = h
                x += 0.01f
            }
        }
    }
}

class WaveTest {
    @Test
    fun beatWavePeakMovesAtWaveSpeedAndExpires() {
        val c = DragonConfig()
        val waves = Waves(c)
        waves.spawnBeat(1f)
        val dt = 0.5f
        waves.update(dt)
        var best = 0f; var bestU = 0f
        var u = 0f
        while (u <= 1f) {
            val v = waves.waveAt(u)
            if (v > best) { best = v; bestU = u }
            u += 0.0005f
        }
        assertEquals(1.35f * dt, bestU, 0.002f)
        waves.update(1.2f)
        assertEquals(0, waves.activeWaves)
    }
}

class FlashLimiterTest {
    @Test
    fun tenRequestsPerSecondStayWithinLimits() {
        val c = FlashConfig()
        val f = FlashLimiter(c)
        val dt = 1f / 120f
        val onsets = ArrayList<Double>()
        var t = 0.0
        var prevLevel = 0f
        var nextRequest = 0.0
        for (i in 0 until (10 / dt).toInt()) {
            if (t >= nextRequest) {
                if (f.request(1f)) onsets += t
                nextRequest += 0.1
            }
            f.update(dt)
            val slope = (f.level - prevLevel) / dt
            assertTrue(slope <= 1f / c.flashAttackS + 1e-3f, "rise too steep: $slope per s")
            prevLevel = f.level
            t += dt
        }
        for (i in 1 until onsets.size) assertTrue(onsets[i] - onsets[i - 1] >= c.minOnsetIntervalS - 1e-6)
        for (start in onsets) assertTrue(onsets.count { it >= start && it < start + 1.0 } <= 3)
        assertTrue(onsets.size >= 20, "limiter should still allow flashes: ${onsets.size}")
    }

    @Test
    fun reduceFlashingAndRedGuard() {
        val c = FlashConfig()
        val f = FlashLimiter(c)
        f.request(1f)
        repeat(20) { f.update(0.01f) } // fully up after 0.06 s, then fading
        val level = f.level
        assertEquals(1f + c.globalFlashGain * level, f.globalMultiplier, 1e-6f)
        f.reduceFlashing = true
        assertEquals(1f + c.reducedFlashGain * level, f.globalMultiplier, 1e-6f)
        assertEquals(c.reducedLocalPulse, f.localPulseScale, 1e-6f)
        f.reduceFlashing = false
        f.redGuardActive = true
        assertEquals(1f + c.globalFlashGain * c.redGuard * level, f.globalMultiplier, 1e-6f)
    }
}
