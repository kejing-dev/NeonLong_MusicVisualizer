package com.technomancer.neonlong.logic.audio

import com.technomancer.neonlong.logic.config.DragonConfig

/** One analysis result, handed from the analysis thread to the frame via a [TripleBuffer]. */
class SpectrumSnapshot {
    val bands = FloatArray(DragonConfig.BANDS)
    var bass = 0f
    var mid = 0f
    var high = 0f
    var vol = 0f
    /** Strength of the strongest beat since the previous snapshot, or 0. */
    var beatStrength = 0f
    /** Strength of the most recent beat (kept until the next one). */
    var lastBeatStrength = 0f
    /** Increments on every beat, so a reader can tell a new beat from one it already used. */
    var beatCount = 0L
    var beatPulse = 0f
    /** True when a source is producing audio; false while the idle generator drives. */
    var hasAudio = false
    /** Source position the analysis window ended at, in seconds. */
    var positionS = 0.0
    /** Increments on every publish. */
    var sequence = 0L

    fun copyFrom(o: SpectrumSnapshot) {
        System.arraycopy(o.bands, 0, bands, 0, bands.size)
        bass = o.bass; mid = o.mid; high = o.high; vol = o.vol
        beatStrength = o.beatStrength; lastBeatStrength = o.lastBeatStrength; beatCount = o.beatCount; beatPulse = o.beatPulse
        hasAudio = o.hasAudio; positionS = o.positionS; sequence = o.sequence
    }
}
