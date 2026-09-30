package com.technomancer.neonlong.logic.input

import com.technomancer.neonlong.logic.config.DragonConfig

/**
 * Everything the simulation reads for one frame, filled in place by the input System.
 * Audio fields stay at zero until audio analysis exists.
 */
class InputSnapshot {
    /** Smoothed band levels, head (32 Hz) to tail (16 kHz). */
    val bands = FloatArray(DragonConfig.BANDS)
    var bass = 0f
    var mid = 0f
    var high = 0f
    var vol = 0f
    /** Strength of a beat detected this frame, or 0. */
    var beatStrength = 0f
    /** Decaying 0..1 pulse after each beat. */
    var beatPulse = 0f
    /** True when a source is producing audio; false means the idle generator drives the dragon. */
    var hasAudio = false
    /** Playback position of the demo or file source, in seconds. */
    var playbackPositionS = 0.0
    var reducedMotion = false

    fun clearAudio() {
        bands.fill(0f)
        bass = 0f; mid = 0f; high = 0f; vol = 0f
        beatStrength = 0f; beatPulse = 0f
        hasAudio = false
    }
}
