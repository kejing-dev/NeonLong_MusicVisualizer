package com.technomancer.neonlong.ecs.systems

import android.util.Log
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.ServiceRegistry

/**
 * Last System each frame. Every 5 s logs `[perf]`: total System time (avg / p95), the
 * dragon step, analysis cost per hop, frames that took over 1.5× the median interval, the
 * material count and flash onsets.
 */
class PerfSystem : System() {
    private val sorted = LongArray(FramePerf.WINDOW)
    private val sortedDt = FloatArray(FramePerf.WINDOW)
    private var lastLogNs = 0L
    private var lastFlashes = 0L

    override fun update(context: SceneUpdateContext) {
        FramePerf.endFrame(context.deltaTime)
        val now = java.lang.System.nanoTime()
        if (now - lastLogNs < INTERVAL_NS) return
        lastLogNs = now
        val n = minOf(FramePerf.frame, FramePerf.WINDOW)
        if (n == 0) return
        var sum = 0L
        var dragonSum = 0L
        for (i in 0 until n) {
            sorted[i] = FramePerf.totalNs[i]
            sum += FramePerf.totalNs[i]
            dragonSum += FramePerf.dragonNs[i]
            sortedDt[i] = FramePerf.intervalS[i]
        }
        java.util.Arrays.sort(sorted, 0, n)
        java.util.Arrays.sort(sortedDt, 0, n)
        val median = sortedDt[n / 2]
        var misses = 0
        for (i in 0 until n) if (sortedDt[i] > median * 1.5f) misses++
        val reg = ServiceRegistry
        val flashes = reg.simulation.flash.acceptedCount
        Log.i(
            LOG_TAG,
            "[perf] systems avg=${sum / n / 1000}us p95=${sorted[(n - 1) * 95 / 100] / 1000}us " +
                "dragon avg=${dragonSum / n / 1000}us | analysis avg=${reg.audio.analysisAvgUs}us max=${reg.audio.analysisMaxUs}us | " +
                "frame median=${(median * 1000).toInt()}ms misses=$misses/$n | materials=${reg.dragonEntities.materialCount} | " +
                "flashes +${flashes - lastFlashes} | audio ${reg.audio.state}",
        )
        lastFlashes = flashes
    }

    private companion object {
        const val INTERVAL_NS = 5_000_000_000L
    }
}
