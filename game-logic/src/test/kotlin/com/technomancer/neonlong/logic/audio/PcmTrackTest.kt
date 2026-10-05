package com.technomancer.neonlong.logic.audio

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PcmTrackTest {
    @Test
    fun readWrapsAroundTheEnd() {
        val t = PcmTrack.ofFloats(floatArrayOf(0f, 1f, 2f, 3f, 4f), 48_000)
        val out = FloatArray(4)
        t.read(3, out, 4)
        assertArrayEquals(floatArrayOf(3f, 4f, 0f, 1f), out)
    }

    @Test
    fun windowEndsJustBeforeTheGivenSample() {
        val t = PcmTrack.ofFloats(floatArrayOf(0f, 1f, 2f, 3f, 4f), 48_000)
        val out = FloatArray(3)
        t.window(4, out)
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), out)
        t.window(1, out) // wraps back past the start
        assertArrayEquals(floatArrayOf(3f, 4f, 0f), out)
    }

    @Test
    fun shortsAreScaledToPlusMinusOne() {
        val t = PcmTrack.ofShorts(shortArrayOf(0, 16384, -32768, 999), count = 3, sampleRate = 44_100)
        assertEquals(3, t.size)
        val out = FloatArray(3)
        t.read(0, out, 3)
        assertArrayEquals(floatArrayOf(0f, 0.5f, -1f), out, 1e-6f)
        assertEquals(3.0 / 44_100, t.durationS, 1e-12)
    }
}
