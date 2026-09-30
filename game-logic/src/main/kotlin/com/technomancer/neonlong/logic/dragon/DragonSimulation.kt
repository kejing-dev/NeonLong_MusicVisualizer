package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.ComfortConfig
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.input.InputSnapshot
import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat
import kotlin.math.exp

/**
 * Advances the dragon one frame and writes every part's pose into a [PoseBuffer].
 * Pure logic: no allocation per tick, no platform calls.
 *
 * Current scope: flight along the loop, the 27 dorsal segment blocks (gray box), touch
 * ripples and light waves, and the Call / Send / Flash commands.
 */
class DragonSimulation(
    private val config: DragonConfig = DragonConfig(),
    private val comfort: ComfortConfig = ComfortConfig(),
) {
    val spine = Spine(config.spineSamples, config.rollAmp)
    val waves = Waves(config)

    /** Head position on the flight path, in path radians. */
    var sHead = 0f
        private set

    /** Simulation time in seconds; only advances while ticking. */
    var time = 0f
        private set

    /** True between Call and Send. */
    var called = false
        private set

    /** Current loop pull toward the user, easing toward [DragonConfig.callOffset] or 0. */
    var pull = 0f
        private set

    /** Seconds the pearl stays parked near the user's hand after a Call. */
    var pearlParkRemaining = 0f
        private set

    /** Front edge the loop is pulled toward: 1.5 units inside the bounds box. */
    val frontZ: Float = config.boundsCenterZ + config.boundsHalfZ - 1.5f

    private val frame = Basis()
    private val rot = Quat()
    private val color = Color3()
    private val baseGlow = Color3(0.05f, 0.05f, 0.06f)
    private val accent = Color3(1.0f, 0.714f, 0.153f) // Kowloon accent #ffb627
    private var pendingFlash = false

    /** Clears motion state, e.g. when the container is rebuilt. */
    fun reset() {
        sHead = 0f
        time = 0f
        called = false
        pull = 0f
        pearlParkRemaining = 0f
        pendingFlash = false
        waves.clear()
    }

    /** Draw the loop toward the user and park the pearl near the hand. */
    fun call() {
        called = true
        pearlParkRemaining = config.pearlParkS
    }

    /** Return the loop to its place. */
    fun send() {
        called = false
        pearlParkRemaining = 0f
    }

    /** A full-strength beat on request (clap or button). */
    fun flash() {
        pendingFlash = true
    }

    /** A touch on segment [segment]; returns false while it's cooling down. */
    fun touchSegment(segment: Int): Boolean = waves.ripple(segment, BodyLayout.segmentU(segment))

    fun tick(dt: Float, input: InputSnapshot, out: PoseBuffer) {
        val step = if (dt < 0f) 0f else if (dt > config.maxStepS) config.maxStepS else dt
        val motion = if (input.reducedMotion) comfort.reducedMotionFactor else 1f
        time += step
        sHead += step * (config.speedBase + config.speedVol * input.vol + config.speedBass * input.bass) * motion

        val targetPull = if (called) config.callOffset else 0f
        pull += (targetPull - pull) * (1f - exp(-config.callBlendRate * step))
        if (pearlParkRemaining > 0f) pearlParkRemaining -= step

        if (pendingFlash) {
            waves.spawnBeat(1.0f)
            pendingFlash = false
        }
        if (input.beatStrength > 0f) waves.spawnBeat(input.beatStrength)
        waves.update(step)

        spine.update(sHead, config.bodyLength, time, input.bass, motion, pull, frontZ)
        writeSegments(out)
    }

    private fun writeSegments(out: PoseBuffer) {
        for (i in 0 until DragonConfig.SEGMENTS) {
            val u = BodyLayout.segmentU(i)
            val r = BodyLayout.radiusAt(u)
            val depth = BodyLayout.segmentDepth(r)
            spine.frameAt(u, frame)
            rot.setFromColumns(frame.s, frame.u, frame.f)
            out.setTransform(PartFamily.SEG_DORSAL, i, frame, rot, 1.9f * r, 1.5f * r, depth)
            val glow = 1.1f * waves.waveAt(u) + waves.flashOf(i)
            color.set(
                baseGlow.r + accent.r * glow,
                baseGlow.g + accent.g * glow,
                baseGlow.b + accent.b * glow,
            )
            out.setAppearance(PartFamily.SEG_DORSAL, i, color, true)
        }
    }
}
