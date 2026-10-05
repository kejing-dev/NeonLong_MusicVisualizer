package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.input.InputSnapshot
import com.technomancer.neonlong.logic.math.Vec3
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PearlFollowTest {
    /** Over a full loop the pearl stays just ahead of the head and never jumps. */
    @Test
    fun pearlLeadsTheHeadSmoothly() {
        val sim = DragonSimulation()
        val pose = PoseBuffer()
        val input = InputSnapshot().apply { bass = 0.5f; vol = 0.5f }
        val last = Vec3()
        val forward = Vec3()
        var minDist = Float.MAX_VALUE
        var maxDist = 0f
        var maxStep = 0f
        var behind = 0
        val frames = 72 * 60
        for (i in 0 until frames) {
            sim.tick(1f / 72f, input, pose)
            val c = sim.pearl.center
            val h = sim.head.frame
            if (i > 72) {
                val d = Vec3().set(c).sub(h.p)
                val dist = d.length()
                minDist = minOf(minDist, dist)
                maxDist = maxOf(maxDist, dist)
                maxStep = maxOf(maxStep, Vec3().set(c).sub(last).length())
                forward.set(h.f)
                if (d.x * forward.x + d.y * forward.y + d.z * forward.z < 0f) behind++
            }
            last.set(c)
        }
        println("[PearlFollowTest] distance $minDist..$maxDist, largest step $maxStep, frames behind the head $behind")
        assertTrue(minDist > 3f, "never inside the head ($minDist)")
        assertTrue(maxDist < 12f, "never far from the head ($maxDist)")
        assertTrue(maxStep < 0.5f, "no jumps ($maxStep per frame)")
        assertTrue(behind == 0, "always in front of the head ($behind frames behind)")
    }
}
