package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.input.InputSnapshot
import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.acos

class ManeAngleTest {
    @Test
    fun bladesStayClearOfTheNeck() {
        val mane = Mane()
        val dir = Vec3()
        val back = Vec3(0f, 0f, -1f) // the neck runs backward from the head
        var mv = 0f
        while (mv <= 2f) {
            for (k in 0 until Mane.BLADES) {
                val row = k % 2
                val phi = -1.7f + 3.4f * (k / 2) / 11f
                mane.headDir(phi, mane.lift(row, mv), dir)
                val deg = Math.toDegrees(acos(dir.dot(back).coerceIn(-1f, 1f).toDouble()))
                assertTrue(deg >= 30.0, "blade $k at mv=$mv is $deg° from the neck")
            }
            mv += 0.05f
        }
    }
}

class JawTest {
    @Test
    fun jawIsCappedAndClosedInSilence() {
        val head = HeadRig()
        head.updateJaw(0f, 0f)
        assertEquals(0f, head.jawAngle, 1e-6f)
        head.updateJaw(1.25f, 1f)
        assertTrue(head.jawAngle <= HeadRig.JAW_MAX + 1e-6f)
        assertEquals(HeadRig.JAW_MAX, head.jawAngle, 1e-6f)
    }
}

class HeadPlacementTest {
    @Test
    fun headSitsAheadOfTheFirstSample() {
        val sim = DragonSimulation()
        val pose = PoseBuffer()
        sim.tick(1f / 72f, InputSnapshot(), pose)
        val f = sim.spine.frameAt(0f, Basis())
        val expected = Vec3().set(f.p).addScaled(f.f, 1.3f)
        val t = pose.transforms(PartFamily.HEAD)
        assertEquals(expected.x, t[0], 1e-5f)
        assertEquals(expected.y, t[1], 1e-5f)
        assertEquals(expected.z, t[2], 1e-5f)
    }
}

class BreathRateIndependenceTest {
    private fun run(hz: Int): Pair<Int, Vec3> {
        val sim = DragonSimulation()
        val pose = PoseBuffer()
        val input = InputSnapshot().apply { bass = 0.9f }
        val dt = 1f / hz
        repeat(10 * hz) { sim.tick(dt, input, pose) }
        return sim.breath.liveCount to sim.breath.meanPosition(Vec3())
    }

    @Test
    fun sixtyAndNinetyHertzAgree() {
        val (n60, m60) = run(60)
        val (n90, m90) = run(90)
        println("[BreathRateIndependenceTest] live 60 Hz=$n60 90 Hz=$n90, mean $m60 vs $m90")
        assertTrue(n60 > 50, "breath should be flowing: $n60")
        assertTrue(kotlin.math.abs(n60 - n90) <= 0.03f * n60, "live counts $n60 vs $n90")
        val d = Vec3().set(m60).sub(m90).length()
        assertTrue(d <= 0.2f, "mean positions differ by $d")
    }
}

class BoundsAllPartsTest {
    @Test
    fun everyPartStaysInTheBox() {
        val config = DragonConfig()
        val sim = DragonSimulation(config)
        val bounds = Bounds(config)
        val pose = PoseBuffer()
        val input = InputSnapshot().apply { bands.fill(1.25f); bass = 1.25f; mid = 1.25f; high = 1.25f; vol = 1.25f; beatPulse = 1f }
        val dt = 1f / 72f
        var sinceBeat = 0f
        var worst = ""
        var escapes = 0
        repeat(20_000) { step ->
            sinceBeat += dt
            input.beatStrength = if (sinceBeat >= 0.27f) { sinceBeat = 0f; 1.5f } else 0f
            sim.tick(dt, input, pose)
            for (fam in PartFamily.entries) {
                val t = pose.transforms(fam)
                val a = pose.appearance(fam)
                for (n in 0 until fam.capacity) {
                    if (a[n * 4 + 3] < 0.5f) continue
                    val o = n * PoseBuffer.TRANSFORM_STRIDE
                    if (!bounds.contains(t[o], t[o + 1], t[o + 2])) {
                        escapes++
                        if (worst.isEmpty()) worst = "step $step $fam #$n at (${t[o]}, ${t[o + 1]}, ${t[o + 2]})"
                    }
                }
            }
            val f = pose.fibers
            for (fib in listOf(f.core) + f.tufts + f.tail + f.pearlFlames + f.cables) {
                for (k in 0 until fib.points) {
                    if (!bounds.contains(fib.xyz[k * 3], fib.xyz[k * 3 + 1], fib.xyz[k * 3 + 2])) {
                        escapes++
                        if (worst.isEmpty()) worst = "step $step fiber point at (${fib.xyz[k * 3]}, ${fib.xyz[k * 3 + 1]}, ${fib.xyz[k * 3 + 2]})"
                    }
                }
            }
        }
        assertEquals(0, escapes, "first escape: $worst")
    }
}
