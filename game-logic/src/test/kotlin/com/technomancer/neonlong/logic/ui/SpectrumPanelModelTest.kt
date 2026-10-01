package com.technomancer.neonlong.logic.ui

import com.technomancer.neonlong.logic.audio.SpectrumSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.management.ManagementFactory

class SpectrumPanelModelTest {
    @Test
    fun hasEightyOneBars() {
        val m = SpectrumPanelModel()
        assertEquals(81, m.bars.size)
        assertEquals(81, m.peaks.size)
    }

    @Test
    fun peakTicksFallMonotonicallyAfterTheBarDrops() {
        val m = SpectrumPanelModel()
        val s = SpectrumSnapshot()
        s.bands[10] = 0.9f
        m.update(s, 1f / 30f)
        assertEquals(0.9f, m.peaks[10], 1e-6f)
        s.bands[10] = 0.1f
        var last = m.peaks[10]
        repeat(120) {
            m.update(s, 1f / 30f)
            assertTrue(m.peaks[10] <= last, "peak never rises while the bar stays low")
            assertTrue(m.peaks[10] >= m.bars[10], "peak never falls below the bar")
            last = m.peaks[10]
        }
        assertEquals(0.1f, m.peaks[10], 1e-6f)
    }

    @Test
    fun updatesAllocateNothing() {
        val mx = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val id = Thread.currentThread().id
        val m = SpectrumPanelModel()
        val s = SpectrumSnapshot()
        repeat(100) { m.update(s, 1f / 30f) }
        val probeA = mx.getThreadAllocatedBytes(id)
        val probeB = mx.getThreadAllocatedBytes(id)
        val overhead = probeB - probeA
        val before = mx.getThreadAllocatedBytes(id)
        var i = 0
        while (i < 1000) {
            s.bands[i % 81] = (i % 7) / 7f
            m.update(s, 1f / 30f)
            i++
        }
        val after = mx.getThreadAllocatedBytes(id)
        assertEquals(0L, after - before - overhead)
    }
}
