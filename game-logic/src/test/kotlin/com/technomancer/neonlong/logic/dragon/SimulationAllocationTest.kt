package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.input.InputSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.lang.management.ManagementFactory

class SimulationAllocationTest {
    @Test
    fun tickAllocatesNothingAfterWarmUp() {
        val mx = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val threadId = Thread.currentThread().id
        val sim = DragonSimulation()
        val input = InputSnapshot().apply { bass = 0.6f; vol = 0.5f }
        val pose = PoseBuffer()
        repeat(200) { sim.tick(1f / 72f, input, pose) }

        // Measure the cost of measuring, then subtract it.
        val probeA = mx.getThreadAllocatedBytes(threadId)
        val probeB = mx.getThreadAllocatedBytes(threadId)
        val overhead = probeB - probeA

        val before = mx.getThreadAllocatedBytes(threadId)
        var i = 0
        while (i < 1000) {
            sim.tick(1f / 72f, input, pose)
            i++
        }
        val after = mx.getThreadAllocatedBytes(threadId)
        assertEquals(0L, after - before - overhead)
    }
}
