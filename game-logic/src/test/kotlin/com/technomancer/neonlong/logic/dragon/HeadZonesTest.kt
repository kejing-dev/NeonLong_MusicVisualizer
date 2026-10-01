package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.input.InputSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HeadZonesTest {
    @Test
    fun blocksMapToTheirColorAndGlow() {
        assertEquals(0, HeadZones.of(0, 1f))
        assertEquals(5, HeadZones.of(11, 1f))
        assertEquals(6, HeadZones.of(4, 0.7f)) // neck collar
        assertEquals(7, HeadZones.of(12, 1.4f)) // nostril
        assertEquals(8, HeadZones.of(13, 1.4f)) // mirrored nostril
    }

    @Test
    fun everyZoneSharesTheHeadPoseAndGlowsItsOwnColor() {
        val pose = PoseBuffer()
        DragonSimulation().tick(1f / 72f, InputSnapshot(), pose)
        val t = pose.transforms(PartFamily.HEAD)
        val a = pose.appearance(PartFamily.HEAD)
        for (z in 1 until HeadZones.COUNT) {
            for (k in 0 until PoseBuffer.TRANSFORM_STRIDE) assertEquals(t[k], t[z * PoseBuffer.TRANSFORM_STRIDE + k], 1e-6f)
        }
        val s = PoseBuffer.APPEARANCE_STRIDE
        // Zones 0 and 5 sit 0.2 apart on the palette, so their colors differ.
        val diff = (0..2).sumOf { kotlin.math.abs(a[it] - a[5 * s + it]).toDouble() }
        assertTrue(diff > 0.01, "zone colors differ ($diff)")
    }
}
