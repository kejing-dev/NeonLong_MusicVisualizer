package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Quat
import com.technomancer.neonlong.logic.math.Vec3

/**
 * Draws a [Fiber] as a chain of [chunks] straight tube pieces (a base-anchored unit cylinder
 * stretched from one sample to the next). Used for tubes while per-frame mesh updates are
 * unproven. Output per chunk: 10 transform floats and 3 color floats.
 */
class ChunkChain(val chunks: Int) {
    val transforms = FloatArray(chunks * 10)
    val rgb = FloatArray(chunks * 3)
    private val a = Vec3()
    private val b = Vec3()
    private val dir = Vec3()
    private val rot = Quat()

    /** Draws the part of [fiber] from point [first] to point [last] (inclusive). */
    fun update(fiber: Fiber, first: Int = 0, last: Int = fiber.points - 1) {
        val span = last - first
        for (k in 0 until chunks) {
            val i0 = first + k * span / chunks
            val i1 = first + (k + 1) * span / chunks
            a.set(fiber.xyz[i0 * 3], fiber.xyz[i0 * 3 + 1], fiber.xyz[i0 * 3 + 2])
            b.set(fiber.xyz[i1 * 3], fiber.xyz[i1 * 3 + 1], fiber.xyz[i1 * 3 + 2])
            dir.set(b).sub(a)
            val len = dir.length()
            dir.normalize()
            rot.setFromUpTo(dir)
            val mid = (i0 + i1) / 2
            val r = fiber.radius[mid]
            val o = k * 10
            transforms[o] = a.x; transforms[o + 1] = a.y; transforms[o + 2] = a.z
            transforms[o + 3] = rot.x; transforms[o + 4] = rot.y; transforms[o + 5] = rot.z; transforms[o + 6] = rot.w
            // Slightly longer than the gap so neighboring pieces overlap without cracks.
            transforms[o + 7] = r; transforms[o + 8] = len * 1.05f; transforms[o + 9] = r
            rgb[k * 3] = fiber.rgb[mid * 3]; rgb[k * 3 + 1] = fiber.rgb[mid * 3 + 1]; rgb[k * 3 + 2] = fiber.rgb[mid * 3 + 2]
        }
    }
}
