package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.ComfortConfig
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.input.InputSnapshot
import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat

/**
 * Advances the dragon one frame and writes every part's pose into a [PoseBuffer].
 * Pure logic: no allocation per tick, no platform calls.
 *
 * Current scope: flight along the loop and the 27 dorsal segment blocks (gray box).
 */
class DragonSimulation(
    private val config: DragonConfig = DragonConfig(),
    private val comfort: ComfortConfig = ComfortConfig(),
) {
    val spine = Spine(config.spineSamples, config.rollAmp)

    /** Head position on the flight path, in path radians. */
    var sHead = 0f
        private set

    /** Simulation time in seconds; only advances while ticking. */
    var time = 0f
        private set

    private val frame = Basis()
    private val rot = Quat()
    private val gray = Color3(0.35f, 0.35f, 0.35f)

    /** Clears motion state, e.g. when the container is rebuilt. */
    fun reset() {
        sHead = 0f
        time = 0f
    }

    fun tick(dt: Float, input: InputSnapshot, out: PoseBuffer) {
        val step = if (dt < 0f) 0f else if (dt > config.maxStepS) config.maxStepS else dt
        val motion = if (input.reducedMotion) comfort.reducedMotionFactor else 1f
        time += step
        sHead += step * (config.speedBase + config.speedVol * input.vol + config.speedBass * input.bass) * motion
        spine.update(sHead, config.bodyLength, time, input.bass, motion)
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
            out.setAppearance(PartFamily.SEG_DORSAL, i, gray, true)
        }
    }
}
