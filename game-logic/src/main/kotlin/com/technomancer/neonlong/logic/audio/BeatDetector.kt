package com.technomancer.neonlong.logic.audio

import com.technomancer.neonlong.logic.config.AudioConfig
import kotlin.math.exp

/**
 * Detects kicks from the low-band energy: a beat is when the current bass level jumps
 * clearly above its running average, is loud enough, and comes long enough after the last.
 */
class BeatDetector(private val c: AudioConfig = AudioConfig()) {
    private var avg = 0f
    private var sinceLast = 1e9f

    /** Decaying 0..1 pulse, set to 1 on each beat. */
    var pulse = 0f
        private set

    /** Strength of the beat found by the last [update], or 0. */
    var beat = 0f
        private set

    /** [raw] is the mean of the unsmoothed band targets 0–9. Returns the beat strength or 0. */
    fun update(raw: Float, dt: Float): Float {
        sinceLast += dt
        pulse *= exp(-c.beatPulseDecay * dt)
        beat = 0f
        if (raw > avg * c.beatRatio + c.beatOffset && raw > c.beatFloor && sinceLast >= c.beatMinInterval) {
            beat = raw + c.beatStrengthBoost
            pulse = 1f
            sinceLast = 0f
        }
        avg += (raw - avg) * (1f - exp(-c.beatAvgRate * dt))
        return beat
    }

    fun reset() {
        avg = 0f
        sinceLast = 1e9f
        pulse = 0f
        beat = 0f
    }
}
