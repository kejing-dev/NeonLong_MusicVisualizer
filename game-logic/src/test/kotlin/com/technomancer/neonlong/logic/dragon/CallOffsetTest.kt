package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.input.InputSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CallOffsetTest {
    @Test
    fun calledLoopStaysInsideBox() {
        val config = DragonConfig()
        val sim = DragonSimulation(config)
        val bounds = Bounds(config)
        val input = InputSnapshot().apply { bass = 1.25f; vol = 1.25f }
        val pose = PoseBuffer()
        sim.call()
        val s = sim.spine.samples
        repeat(20_000) { step ->
            sim.tick(1f / 72f, input, pose)
            for (k in 0..config.spineSamples) {
                val i = k * 3
                assertTrue(bounds.contains(s[i], s[i + 1], s[i + 2]), "step $step sample $k z=${s[i + 2]}")
            }
        }
        assertEquals(config.callOffset, sim.pull, 1e-3f)
    }

    @Test
    fun callMovesLoopTowardUserAndSendReturnsIt() {
        val config = DragonConfig()
        val sim = DragonSimulation(config)
        val input = InputSnapshot()
        val pose = PoseBuffer()
        fun meanZ(): Float {
            // Average over a full loop so the flight path's own motion cancels out.
            var sum = 0.0
            var n = 0
            repeat(2400) {
                sim.tick(1f / 72f, input, pose)
                val s = sim.spine.samples
                for (k in 0..config.spineSamples) { sum += s[k * 3 + 2]; n++ }
            }
            return (sum / n).toFloat()
        }
        val before = meanZ()
        sim.call()
        repeat(300) { sim.tick(1f / 72f, input, pose) }
        val called = meanZ()
        assertTrue(called > before + 2f, "loop center moved toward the user: $before -> $called")
        assertTrue(sim.pearlParkRemaining <= 0f, "pearl parking ends after 5 s")
        sim.send()
        repeat(600) { sim.tick(1f / 72f, input, pose) }
        assertEquals(0f, sim.pull, 1e-3f)
    }
}
