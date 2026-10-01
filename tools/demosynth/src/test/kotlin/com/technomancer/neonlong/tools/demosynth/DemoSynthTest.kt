package com.technomancer.neonlong.tools.demosynth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToLong

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoSynthTest {
    private lateinit var track: DemoTrack

    @BeforeAll
    fun render() {
        track = renderDemo()
    }

    @Test
    fun duration() {
        val expected = (36.923077 * SAMPLE_RATE).roundToLong()
        assertTrue(abs(track.samples.size - expected) <= 1, "samples ${track.samples.size} vs $expected")
    }

    @Test
    fun peakAtMinusOneDbfs() {
        var peak = 0f
        for (v in track.samples) peak = maxOf(peak, abs(v))
        val db = 20 * log10(peak.toDouble())
        assertTrue(db in -1.1..-0.9, "peak $db dBFS")
    }

    @Test
    fun kicks() {
        assertEquals(54, track.kicks.size)
        assertEquals(4.615, track.kicks.first(), 0.001)
        for (k in track.kicks) {
            val steps = k / 0.14423077
            assertEquals(Math.round(steps).toDouble(), steps, 0.001 / 0.14423077, "kick at $k is off the grid")
        }
    }

    @Test
    fun deterministic() {
        val again = renderDemo()
        assertTrue(track.samples.contentEquals(again.samples))
    }
}
