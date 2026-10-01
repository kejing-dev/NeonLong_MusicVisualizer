package com.technomancer.neonlong.ecs.systems

/**
 * Per-System timing shared by the frame Systems and reported by [PerfSystem].
 * Each System adds its nanoseconds for the current frame; PerfSystem closes the frame.
 */
object FramePerf {
    const val WINDOW = 360
    /** Total System time per frame over the last [WINDOW] frames. */
    val totalNs = LongArray(WINDOW)
    val dragonNs = LongArray(WINDOW)
    val intervalS = FloatArray(WINDOW)
    var frame = 0
        private set
    @Volatile var currentTotalNs = 0L
    var currentDragonNs = 0L

    fun add(ns: Long) {
        currentTotalNs += ns
    }

    /** Stores this frame's totals and starts the next frame. */
    fun endFrame(dt: Float) {
        val i = frame % WINDOW
        totalNs[i] = currentTotalNs
        dragonNs[i] = currentDragonNs
        intervalS[i] = dt
        currentTotalNs = 0
        currentDragonNs = 0
        frame++
    }
}
