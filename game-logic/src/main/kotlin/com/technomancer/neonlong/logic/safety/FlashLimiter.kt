package com.technomancer.neonlong.logic.safety

import com.technomancer.neonlong.logic.config.FlashConfig
import kotlin.math.exp

/**
 * Gates every dragon-wide flash so the light never flickers dangerously: at most
 * [FlashConfig.maxFlashesPerSecond] onsets in any one-second window, a minimum gap between
 * onsets, a ramped rise (never a jump) and a smooth fade. Produces the envelope [level] ∈ [0, 1].
 */
class FlashLimiter(private val c: FlashConfig = FlashConfig()) {
    private val onsets = DoubleArray(c.maxFlashesPerSecond) { Double.NEGATIVE_INFINITY }
    private var onsetCursor = 0
    private var time = 0.0
    private var target = 0f
    private var rising = false

    /** Current envelope, 0..1. */
    var level = 0f
        private set

    /** Onsets accepted so far (for the flash audit). */
    var acceptedCount = 0L
        private set

    var reduceFlashing = false
    var redGuardActive = false

    /** Multiplier for every emissive gain: 1 + gain · level. */
    val globalMultiplier: Float
        get() {
            var g = if (reduceFlashing) c.reducedFlashGain else c.globalFlashGain
            if (redGuardActive) g *= c.redGuard
            return 1f + g * level
        }

    /** Scale for local beat effects (beacons and the like). */
    val localPulseScale: Float get() = if (reduceFlashing) c.reducedLocalPulse else 1f

    /** Asks for a flash of [strength] (0..1). Returns true if it was allowed to start. */
    fun request(strength: Float): Boolean {
        val lastOnset = onsets[(onsetCursor + onsets.size - 1) % onsets.size]
        if (time - lastOnset < c.minOnsetIntervalS) return false
        // The oldest of the last N onsets must be over a second ago.
        val oldest = onsets[onsetCursor]
        if (time - oldest < 1.0) return false
        onsets[onsetCursor] = time
        onsetCursor = (onsetCursor + 1) % onsets.size
        target = if (strength > 1f) 1f else if (strength < 0f) 0f else strength
        rising = true
        acceptedCount++
        return true
    }

    fun update(dt: Float) {
        time += dt
        if (rising) {
            level += dt / c.flashAttackS
            if (level >= target) {
                level = target
                rising = false
            }
        } else {
            level *= exp(-dt / c.flashDecayS)
        }
    }

    fun reset() {
        onsets.fill(Double.NEGATIVE_INFINITY)
        onsetCursor = 0
        level = 0f
        rising = false
    }
}
