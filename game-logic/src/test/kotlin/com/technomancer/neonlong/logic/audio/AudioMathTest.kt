package com.technomancer.neonlong.logic.audio

import com.technomancer.neonlong.logic.config.AudioConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class FftTest {
    @Test
    fun sineAtBin100PeaksAtBin100() {
        val n = 4096
        val re = FloatArray(n) { sin(2 * PI * 100 * it / n).toFloat() }
        val im = FloatArray(n)
        Fft(n).forward(re, im)
        var best = 0
        var bestMag = 0f
        for (k in 0 until n / 2) {
            val m = sqrt(re[k] * re[k] + im[k] * im[k])
            if (m > bestMag) { bestMag = m; best = k }
        }
        assertEquals(100, best)
    }

    @Test
    fun parseval() {
        val n = 4096
        val rng = com.technomancer.neonlong.logic.math.Rng(5)
        val x = FloatArray(n) { (rng.next() * 2 - 1).toFloat() }
        val re = x.copyOf()
        val im = FloatArray(n)
        Fft(n).forward(re, im)
        var time = 0.0
        var freq = 0.0
        for (i in 0 until n) {
            time += x[i].toDouble() * x[i]
            freq += re[i].toDouble() * re[i] + im[i].toDouble() * im[i]
        }
        assertEquals(1.0, freq / n / time, 1e-3)
    }
}

class SpectrumAnalyzerTest {
    private val rate = 48_000
    private val c = AudioConfig()

    private fun sine(freq: Double, amp: Double) = FloatArray(c.fftSize) { (amp * sin(2 * PI * freq * it / rate)).toFloat() }

    @Test
    fun sineMatchesPredictedLevel() {
        val a = SpectrumAnalyzer(c)
        val bin = 85
        val freq = bin * rate.toDouble() / c.fftSize // ~996 Hz, centered on a bin
        val amp = 0.05
        val s = sine(freq, amp)
        repeat(60) { a.analyzeWindow(s) }
        // Blackman coherent gain is 0.42; a real sine puts half its amplitude in the bin.
        val mag = amp * 0.42 / 2
        val expected = ((20 * log10(mag) - c.minDb) / (c.maxDb - c.minDb)).toFloat()
        assertEquals(expected, a.normalized[bin], 0.01f)
    }

    @Test
    fun loudSineClampsToOne() {
        val a = SpectrumAnalyzer(c)
        val s = sine(85 * rate.toDouble() / c.fftSize, 0.5)
        repeat(60) { a.analyzeWindow(s) }
        assertEquals(1f, a.normalized[85], 0.01f)
    }

    @Test
    fun silenceDecaysBySmoothingFactor() {
        val a = SpectrumAnalyzer(c)
        val s = sine(85 * rate.toDouble() / c.fftSize, 0.5)
        repeat(60) { a.analyzeWindow(s) }
        val start = a.smoothedMagnitude(85)
        val zeros = FloatArray(c.fftSize)
        repeat(5) { a.analyzeWindow(zeros) }
        assertEquals(start * Math.pow(0.7, 5.0).toFloat(), a.smoothedMagnitude(85), start * 1e-4f)
    }
}

class BandMapperTest {
    private fun check(rate: Int) {
        val m = BandMapper(rate, 4096)
        val binHz = rate.toDouble() / 4096
        assertEquals(81, m.count)
        for (i in 0 until m.count) {
            assertTrue(m.hi[i] - m.lo[i] >= 1, "band $i has no bins")
            if (i > 0) assertTrue(m.lo[i] >= m.lo[i - 1], "lo decreases at band $i")
        }
        assertTrue(m.lo[0] * binHz <= 32.0 && 32.0 < m.hi[0] * binHz, "band 0 contains 32 Hz")
        assertTrue(m.hi[80] * binHz >= 15_900.0, "band 80 ends at ${m.hi[80] * binHz} Hz")
    }

    @Test fun at44100() = check(44_100)
    @Test fun at48000() = check(48_000)
}

class FeatureExtractorTest {
    private val dt = 1f / 60f

    @Test
    fun attackReachesSixtyThreePercentInOneThirtySecond() {
        val f = FeatureExtractor()
        val raw = FloatArray(81) { 1f }
        var t = 0f
        var crossed = -1f
        repeat(10) {
            f.update(raw, dt); t += dt
            if (crossed < 0 && f.bands[0] >= 0.632f * f.targets[0]) crossed = t
        }
        assertEquals(1f / 32f, crossed, dt)
    }

    @Test
    fun releaseFallsToThirtySevenPercentInOneSeventhSecond() {
        val f = FeatureExtractor()
        val on = FloatArray(81) { 1f }
        repeat(120) { f.update(on, dt) }
        val start = f.bands[0]
        val off = FloatArray(81)
        var t = 0f
        var crossed = -1f
        repeat(30) {
            f.update(off, dt); t += dt
            if (crossed < 0 && f.bands[0] <= 0.368f * start) crossed = t
        }
        assertEquals(1f / 7f, crossed, dt)
    }

    @Test
    fun rangesAreExact() {
        val f = FeatureExtractor()
        val v = FloatArray(81)
        for (i in 0..9) v[i] = 1f
        f.setDirect(v)
        assertEquals(1f, f.bass, 1e-6f); assertEquals(0f, f.mid, 1e-6f); assertEquals(0f, f.high, 1e-6f)
        v.fill(0f); for (i in 28..51) v[i] = 1f
        f.setDirect(v)
        assertEquals(1f, f.mid, 1e-6f); assertEquals(0f, f.bass, 1e-6f)
        v.fill(0f); for (i in 55..80) v[i] = 1f
        f.setDirect(v)
        assertEquals(1f, f.high, 1e-6f)
        assertEquals(26f / 81f, f.vol, 1e-6f)
    }
}

class IdleGeneratorTest {
    @Test
    fun valuesInRangeAndNoBeats() {
        val p = AnalysisPipeline(48_000)
        val snap = SpectrumSnapshot()
        val dt = 1f / 60f
        var t = 0f
        repeat(60 * 60) {
            p.hopIdle(t, dt, snap)
            for (v in snap.bands) assertTrue(v in 0f..0.8f, "idle value $v")
            assertEquals(0f, snap.beatStrength)
            t += dt
        }
        assertEquals(0L, snap.beatCount)
    }
}

class PanLawTest {
    @Test
    fun equalPower() {
        val g = FloatArray(2)
        var deg = -90.0
        while (deg <= 90.0) {
            PanLaw.gains(Math.toRadians(deg).toFloat(), g)
            assertEquals(1f, g[0] * g[0] + g[1] * g[1], 1e-6f)
            deg += 1.0
        }
        PanLaw.gains(Math.toRadians(90.0).toFloat(), g)
        assertEquals(0f, g[0], 1e-6f)
    }
}

class TripleBufferTest {
    @Test
    fun readerSeesNewestPublished() {
        val tb = TripleBuffer { IntArray(1) }
        tb.writeSlot[0] = 1; tb.publish()
        tb.writeSlot[0] = 2; tb.publish()
        assertEquals(2, tb.read()[0])
        assertEquals(2, tb.read()[0], "no new publish: same value")
        tb.writeSlot[0] = 3; tb.publish()
        assertEquals(3, tb.read()[0])
    }
}
