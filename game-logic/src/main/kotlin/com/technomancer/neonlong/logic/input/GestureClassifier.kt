package com.technomancer.neonlong.logic.input

import com.technomancer.neonlong.logic.math.Vec3

/**
 * Recognizes the three hand gestures from per-frame hand data:
 * - Call: an open palm facing the dragon, held for [palmHoldS].
 * - Send: a sideways palm swipe faster than [swipeSpeed] over at least [swipeDist].
 * - Clap: palms closing faster than [clapSpeed] to within [clapDist].
 * Each gesture then waits [cooldownS] before it can fire again. No allocation per update.
 */
class GestureClassifier(
    private val palmHoldS: Float = 0.5f,
    private val swipeSpeed: Float = 1.2f,
    private val swipeDist: Float = 0.25f,
    private val clapDist: Float = 0.06f,
    private val clapSpeed: Float = 0.8f,
    private val cooldownS: Float = 0.6f,
    /** Cosine of the widest angle between palm normal and the direction to the dragon. */
    private val facingCos: Float = 0.7f,
) {
    enum class Gesture { NONE, CALL, SEND, CLAP }

    private val hands = arrayOf(HandTrack(), HandTrack())
    private var prevPalmDist = -1f
    private var clapClosingSpeed = 0f
    private var cooldownCall = 0f
    private var cooldownSend = 0f
    private var cooldownClap = 0f
    private val tmp = Vec3()

    /**
     * Feeds one frame. [target] is the dragon's position. Returns the gesture recognized
     * this frame (at most one; clap wins over send, send over call).
     */
    fun update(dt: Float, left: HandFrame, right: HandFrame, target: Vec3): Gesture {
        if (dt <= 0f) return Gesture.NONE
        cooldownCall -= dt; cooldownSend -= dt; cooldownClap -= dt

        var result = Gesture.NONE
        if (detectClap(dt, left, right) && cooldownClap <= 0f) {
            cooldownClap = cooldownS
            result = Gesture.CLAP
        }
        for (h in 0..1) {
            val frame = if (h == 0) left else right
            val track = hands[h]
            val swiped = track.updateSwipe(dt, frame, target)
            val held = track.updatePalmHold(dt, frame, target)
            if (result == Gesture.NONE && swiped && cooldownSend <= 0f) {
                cooldownSend = cooldownS
                result = Gesture.SEND
            }
            if (result == Gesture.NONE && held && cooldownCall <= 0f) {
                cooldownCall = cooldownS
                result = Gesture.CALL
            }
        }
        return result
    }

    fun reset() {
        hands[0].reset(); hands[1].reset()
        prevPalmDist = -1f
        clapClosingSpeed = 0f
        cooldownCall = 0f; cooldownSend = 0f; cooldownClap = 0f
    }

    private fun detectClap(dt: Float, left: HandFrame, right: HandFrame): Boolean {
        if (!left.tracked || !right.tracked) {
            prevPalmDist = -1f
            return false
        }
        val d = tmp.set(left.palm).sub(right.palm).length()
        var fired = false
        if (prevPalmDist >= 0f) {
            val closing = (prevPalmDist - d) / dt
            // Fire on the frame the palms cross into clap distance, judged by the speed
            // they were closing at just before.
            if (d < clapDist && prevPalmDist >= clapDist && maxOf(closing, clapClosingSpeed) > clapSpeed) fired = true
            clapClosingSpeed = closing
        }
        prevPalmDist = d
        return fired
    }

    /** Per-hand history for the swipe and the palm hold. */
    private inner class HandTrack {
        private val prevPalm = Vec3()
        private var hasPrev = false
        private var swipeTravel = 0f
        private var holdTime = 0f
        private val toTarget = Vec3()
        private val lateral = Vec3()
        private val up = Vec3(0f, 1f, 0f)

        fun reset() {
            hasPrev = false; swipeTravel = 0f; holdTime = 0f
        }

        /** True once the palm has moved sideways fast enough for far enough. */
        fun updateSwipe(dt: Float, frame: HandFrame, target: Vec3): Boolean {
            if (!frame.tracked) {
                hasPrev = false; swipeTravel = 0f
                return false
            }
            var fired = false
            if (hasPrev) {
                // Sideways = horizontal and perpendicular to the direction to the dragon.
                toTarget.set(target).sub(frame.palm)
                toTarget.y = 0f
                toTarget.normalize()
                lateral.cross(up, toTarget).normalize()
                val dx = frame.palm.x - prevPalm.x
                val dy = frame.palm.y - prevPalm.y
                val dz = frame.palm.z - prevPalm.z
                val side = dx * lateral.x + dy * lateral.y + dz * lateral.z
                val speed = kotlin.math.abs(side) / dt
                if (speed > swipeSpeed) {
                    swipeTravel += kotlin.math.abs(side)
                    if (swipeTravel >= swipeDist) {
                        fired = true
                        swipeTravel = 0f
                    }
                } else {
                    swipeTravel = 0f
                }
            }
            prevPalm.set(frame.palm)
            hasPrev = true
            return fired
        }

        /** True once an open palm has faced the dragon for the hold time. */
        fun updatePalmHold(dt: Float, frame: HandFrame, target: Vec3): Boolean {
            if (!frame.tracked || !frame.open) {
                holdTime = 0f
                return false
            }
            toTarget.set(target).sub(frame.palm)
            val len = toTarget.length()
            val facing = if (len > 1e-6f) frame.palmNormal.dot(toTarget) / len else 0f
            if (facing < facingCos) {
                holdTime = 0f
                return false
            }
            val before = holdTime
            holdTime += dt
            // Fire once, on the frame the hold crosses the threshold.
            return before < palmHoldS && holdTime >= palmHoldS - 1e-4f
        }
    }
}
