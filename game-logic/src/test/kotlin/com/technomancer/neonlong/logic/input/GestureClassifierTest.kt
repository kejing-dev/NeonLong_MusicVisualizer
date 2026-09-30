package com.technomancer.neonlong.logic.input

import com.technomancer.neonlong.logic.input.GestureClassifier.Gesture
import com.technomancer.neonlong.logic.math.Vec3
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class GestureClassifierTest {
    private val dt = 1f / 72f
    private val target = Vec3(0f, 1f, -1f)

    private fun untracked() = HandFrame()

    /** Runs [frames] frames, letting [setup] place the hands, and collects the gestures seen. */
    private fun run(frames: Int, setup: (i: Int, left: HandFrame, right: HandFrame) -> Unit): List<Gesture> {
        val g = GestureClassifier()
        val left = HandFrame()
        val right = HandFrame()
        val seen = ArrayList<Gesture>()
        for (i in 0 until frames) {
            setup(i, left, right)
            val r = g.update(dt, left, right, target)
            if (r != Gesture.NONE) seen += r
        }
        return seen
    }

    private fun palmFacingTarget(h: HandFrame) {
        h.tracked = true
        h.open = true
        h.palm.set(0f, 1.1f, 0.4f)
        h.palmNormal.set(target).sub(h.palm).normalize()
    }

    @Test
    fun palmHeldHalfSecondCalls() {
        val seen = run(frames = 38) { _, left, right -> palmFacingTarget(right); left.tracked = false }
        assertEquals(listOf(Gesture.CALL), seen)
    }

    @Test
    fun palmHeldFortyMillisShortDoesNotCall() {
        val seen = run(frames = (0.4f * 72).toInt()) { _, left, right -> palmFacingTarget(right); left.tracked = false }
        assertEquals(emptyList<Gesture>(), seen)
    }

    @Test
    fun palmFacingAwayDoesNotCall() {
        val seen = run(frames = 72) { _, left, right ->
            palmFacingTarget(right)
            right.palmNormal.scale(-1f)
            left.tracked = false
        }
        assertEquals(emptyList<Gesture>(), seen)
    }

    private fun swipe(speed: Float, distance: Float): List<Gesture> {
        val frames = (distance / speed / dt).toInt() + 2
        return run(frames) { i, left, right ->
            left.tracked = false
            right.tracked = true
            right.open = false
            right.palm.set(-0.2f + speed * dt * i, 1.1f, 0.3f)
        }
    }

    @Test
    fun fastSwipeSends() = assertEquals(listOf(Gesture.SEND), swipe(1.5f, 0.3f))

    @Test
    fun slowSwipeDoesNothing() = assertEquals(emptyList<Gesture>(), swipe(0.9f, 0.3f))

    private fun clap(closingSpeed: Float, endDist: Float): List<Gesture> {
        val start = 0.3f
        val frames = ((start - endDist) / closingSpeed / dt).toInt() + 1
        return run(frames) { i, left, right ->
            val d = maxOf(endDist, start - closingSpeed * dt * i)
            left.tracked = true; right.tracked = true
            left.open = false; right.open = false
            left.palm.set(-d / 2f, 1.1f, 0.3f)
            right.palm.set(d / 2f, 1.1f, 0.3f)
        }
    }

    @Test
    fun fastClap() = assertEquals(listOf(Gesture.CLAP), clap(1.0f, 0.05f))

    @Test
    fun slowClapDoesNothing() = assertEquals(emptyList<Gesture>(), clap(0.3f, 0.05f))

    @Test
    fun cooldownBlocksRepeatWithinSixTenths() {
        // Two claps 0.4 s apart: only the first counts.
        val period = (0.4f / dt).toInt()
        val seen = run(frames = period * 2) { i, left, right ->
            val phase = i % period
            val d = if (phase < period / 2) 0.3f - 1.5f * dt * phase else 0.3f
            left.tracked = true; right.tracked = true
            left.palm.set(-maxOf(0.04f, d) / 2f, 1.1f, 0.3f)
            right.palm.set(maxOf(0.04f, d) / 2f, 1.1f, 0.3f)
        }
        assertEquals(listOf(Gesture.CLAP), seen)
    }

    @Test
    fun untrackedHandsNeverFire() {
        val g = GestureClassifier()
        repeat(200) { assertFalse(g.update(dt, untracked(), untracked(), target) != Gesture.NONE) }
    }
}
