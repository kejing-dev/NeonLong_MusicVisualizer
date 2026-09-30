package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import kotlin.math.exp
import kotlin.math.min

/**
 * Pulses of light travelling along the body. Beats start one wave at the head moving to
 * the tail; touching a segment starts two waves moving both ways from that segment and
 * briefly brightens it. Fixed-size pool, no allocation.
 */
class Waves(private val c: DragonConfig, capacity: Int = 48) {
    private val origin = FloatArray(capacity)
    private val direction = FloatArray(capacity)
    private val speed = FloatArray(capacity)
    private val strength = FloatArray(capacity)
    private val age = FloatArray(capacity)
    private var count = 0

    private val segmentFlash = FloatArray(DragonConfig.SEGMENTS)
    private val segmentCooldown = FloatArray(DragonConfig.SEGMENTS)

    val activeWaves: Int get() = count

    /** One wave from the head toward the tail, e.g. on a beat. */
    fun spawnBeat(s: Float) {
        add(0f, 1f, c.waveSpeed, min(c.waveMaxStrength, s))
    }

    /**
     * Two waves outward from segment [segment] at spine position [u], plus a glow on that
     * segment. Returns false while the segment is cooling down.
     */
    fun ripple(segment: Int, u: Float): Boolean {
        if (segmentCooldown[segment] > 0f) return false
        add(u, 1f, c.rippleSpeed, c.rippleStrength)
        add(u, -1f, c.rippleSpeed, c.rippleStrength)
        segmentFlash[segment] = c.rippleFlash
        segmentCooldown[segment] = c.rippleCooldownS
        return true
    }

    fun update(dt: Float) {
        var i = 0
        while (i < count) {
            age[i] += dt
            if (age[i] >= c.waveLifeS) removeAt(i) else i++
        }
        val decay = exp(-c.rippleFlashDecay * dt)
        for (s in segmentFlash.indices) {
            segmentFlash[s] *= decay
            if (segmentCooldown[s] > 0f) segmentCooldown[s] -= dt
        }
    }

    /** Summed wave brightness at spine position [u]. */
    fun waveAt(u: Float): Float {
        var sum = 0f
        for (i in 0 until count) {
            val center = origin[i] + direction[i] * speed[i] * age[i]
            val d = u - center
            sum += strength[i] * exp(-d * d / c.waveWidth)
        }
        return sum
    }

    fun flashOf(segment: Int): Float = segmentFlash[segment]

    fun clear() {
        count = 0
        segmentFlash.fill(0f)
        segmentCooldown.fill(0f)
    }

    private fun add(u0: Float, dir: Float, v: Float, s: Float) {
        if (count == origin.size) removeAt(0) // drop the oldest
        origin[count] = u0; direction[count] = dir; speed[count] = v; strength[count] = s; age[count] = 0f
        count++
    }

    private fun removeAt(i: Int) {
        val last = count - 1
        origin[i] = origin[last]; direction[i] = direction[last]; speed[i] = speed[last]
        strength[i] = strength[last]; age[i] = age[last]
        count--
    }
}
