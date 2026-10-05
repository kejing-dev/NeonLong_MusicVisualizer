package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.input.InputSnapshot
import com.technomancer.neonlong.logic.math.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.management.ManagementFactory
import kotlin.math.cos
import kotlin.math.sin

class BreathFreeFlightTest {
    private fun blowing() = InputSnapshot().apply { bass = 0.9f; vol = 0.6f }

    /** Particles live in the room: turning the dragon doesn't move the ones already in the air. */
    @Test
    fun turningTheDragonLeavesParticlesWhereTheyAre() {
        val sim = DragonSimulation()
        val pose = PoseBuffer()
        val input = blowing()
        repeat(60) { sim.tick(1f / 72f, input, pose) }
        val b = sim.breath
        val i = (0 until b.capacity).first { b.isLive(it) }
        val x0 = b.x(i); val y0 = b.y(i); val z0 = b.z(i)
        // A big turn with (almost) no time passing and nothing new emitted.
        input.dragonYaw = 1.2f
        input.bass = 0f
        sim.tick(1e-6f, input, pose)
        assertEquals(x0, b.x(i), 1e-3f)
        assertEquals(y0, b.y(i), 1e-3f)
        assertEquals(z0, b.z(i), 1e-3f)
    }

    /** A new particle starts at the mouth as the room sees it: the model position turned by the yaw. */
    @Test
    fun particlesStartAtTheMouthInTheRoom() {
        val cfg = DragonConfig()
        val sim = DragonSimulation(cfg)
        val pose = PoseBuffer()
        val input = InputSnapshot().apply { dragonYaw = 0.7f }
        repeat(30) { sim.tick(1f / 72f, input, pose) }
        input.bass = 0.9f
        sim.tick(1e-6f, input, pose) // no emission yet at this tiny step, but it records the mouth
        input.beatStrength = 1f
        sim.tick(1e-6f, input, pose) // a beat burst, with no time to move
        val b = sim.breath
        val i = (0 until b.capacity).first { b.isLive(it) }
        val mouth = sim.head.toModel(HeadRig.MOUTH_X, HeadRig.MOUTH_Y, HeadRig.MOUTH_Z, Vec3())
        val rx = mouth.x - cfg.boundsCenterX
        val rz = mouth.z - cfg.boundsCenterZ
        assertEquals(cfg.boundsCenterX + rx * cos(0.7f) + rz * sin(0.7f), b.x(i), 1e-2f)
        assertEquals(mouth.y, b.y(i), 1e-2f)
        assertEquals(cfg.boundsCenterZ - rx * sin(0.7f) + rz * cos(0.7f), b.z(i), 1e-2f)
    }

    /** A particle's slot is freed once it has faded out, and the pool never allocates. */
    @Test
    fun fadedParticlesAreFreedAndNothingIsAllocated() {
        val sim = DragonSimulation()
        val pose = PoseBuffer()
        val input = blowing()
        repeat(200) { sim.tick(1f / 72f, input, pose) }
        assertTrue(sim.breath.liveCount > 50, "blowing")

        val mx = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val id = Thread.currentThread().id
        val probeA = mx.getThreadAllocatedBytes(id)
        val probeB = mx.getThreadAllocatedBytes(id)
        val before = mx.getThreadAllocatedBytes(id)
        var k = 0
        while (k < 1000) {
            sim.tick(1f / 72f, input, pose)
            k++
        }
        val after = mx.getThreadAllocatedBytes(id)
        assertEquals(0L, after - before - (probeB - probeA))

        // Stop blowing: within a little over one lifetime every slot is free again.
        input.bass = 0f
        input.vol = 0f
        repeat((72 * (Breath.LIFE_S + 0.2f)).toInt()) { sim.tick(1f / 72f, input, pose) }
        assertEquals(0, sim.breath.liveCount)
    }

    /** The throw alone outruns the head's own speed being added, so the head can't catch its breath. */
    @Test
    fun throwNumbersAreSane() {
        assertTrue(Breath.SPEED_MIN / Breath.DRAG in 5f..11f, "shortest throw")
        assertTrue(Breath.SPEED_MAX / Breath.DRAG in 5f..11f, "longest throw")
    }
}
