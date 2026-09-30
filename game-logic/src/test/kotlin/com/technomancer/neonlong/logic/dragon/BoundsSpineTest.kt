package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.input.InputSnapshot
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoundsSpineTest {
    @Test
    fun spineStaysInsideBoxAtMaxInput() {
        val config = DragonConfig()
        val sim = DragonSimulation(config)
        val bounds = Bounds(config)
        val input = InputSnapshot().apply { bass = 1.25f; vol = 1.25f }
        val pose = PoseBuffer()
        val samples = sim.spine.samples
        repeat(20_000) { step ->
            sim.tick(1f / 72f, input, pose)
            for (k in 0..config.spineSamples) {
                val i = k * 3
                assertTrue(
                    bounds.contains(samples[i], samples[i + 1], samples[i + 2]),
                    "step $step sample $k at (${samples[i]}, ${samples[i + 1]}, ${samples[i + 2]})",
                )
            }
        }
    }
}
