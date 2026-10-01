package com.technomancer.neonlong.logic.ui

import com.technomancer.neonlong.logic.audio.SpectrumSnapshot
import com.technomancer.neonlong.logic.config.DragonConfig

/**
 * What the spectrum panel draws: 81 bar heights, a peak tick above each bar that falls back
 * slowly, and Low / Mid / High meters. Updated in place; allocates nothing after construction.
 */
class SpectrumPanelModel(
    /** How fast a peak tick falls, in bar heights per second. */
    private val peakFall: Float = 0.6f,
    /** How long a peak tick holds before it starts falling, in seconds. */
    private val peakHold: Float = 0.25f,
) {
    val bars = FloatArray(DragonConfig.BANDS)
    val peaks = FloatArray(DragonConfig.BANDS)
    private val holdLeft = FloatArray(DragonConfig.BANDS)
    var low = 0f
        private set
    var mid = 0f
        private set
    var high = 0f
        private set

    /** Copies the latest analysis and moves the peak ticks on by [dt] seconds. */
    fun update(s: SpectrumSnapshot, dt: Float) {
        for (i in bars.indices) {
            val v = s.bands[i].coerceIn(0f, 1f)
            bars[i] = v
            if (v >= peaks[i]) {
                peaks[i] = v
                holdLeft[i] = peakHold
            } else if (holdLeft[i] > 0f) {
                holdLeft[i] -= dt
            } else {
                peaks[i] = (peaks[i] - peakFall * dt).coerceAtLeast(v)
            }
        }
        low = s.bass.coerceIn(0f, 1f)
        mid = s.mid.coerceIn(0f, 1f)
        high = s.high.coerceIn(0f, 1f)
    }

    fun reset() {
        bars.fill(0f); peaks.fill(0f); holdLeft.fill(0f)
        low = 0f; mid = 0f; high = 0f
    }
}
