package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.math.Basis
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * The 81 back towers, three per segment: tower n = 3i + j reads band n, so the head shows
 * the lowest frequencies and the tail the highest. Holds each tower's base, width and height
 * for the current frame; [TowerClearance] keeps neighbors from touching.
 */
class Towers {
    val count = DragonConfig.TOWERS
    val baseX = FloatArray(count); val baseY = FloatArray(count); val baseZ = FloatArray(count)
    val width = FloatArray(count)
    val height = FloatArray(count)
    val spire = FloatArray(count)
    val cap = FloatArray(count)

    /** Places tower bases on each segment's frame (radius [radius] and depth [depth] per segment). */
    fun layout(frames: Array<Basis>, radius: FloatArray, depth: FloatArray) {
        for (i in 0 until DragonConfig.SEGMENTS) {
            val f = frames[i]
            val r = radius[i]
            val d = depth[i]
            for (j in 0..2) {
                val n = 3 * i + j
                val side = (j - 1) * 0.33f * r
                val fwd = (1 - j) * 0.26f * d
                val up = 1.05f * r
                baseX[n] = f.p.x + up * f.u.x + side * f.s.x + fwd * f.f.x
                baseY[n] = f.p.y + up * f.u.y + side * f.s.y + fwd * f.f.y
                baseZ[n] = f.p.z + up * f.u.z + side * f.s.z + fwd * f.f.z
                width[n] = r * (if (j == 1) 0.30f else 0.24f)
            }
        }
    }

    /**
     * Sets heights from band values [v] (per tower) and body radius, soft-capped by
     * [cap]: body H and spire scale down together so H + spire + 0.2 approaches the cap
     * smoothly instead of hitting it.
     */
    fun size(v: FloatArray, radius: FloatArray) {
        for (n in 0 until count) {
            val r = radius[n / 3]
            var h = (0.2f + 3.3f * v[n]) * (0.5f + 0.5f * r / 1.45f)
            var s = 0.15f + 0.7f * v[n]
            val tot = h + s + 0.2f
            val c = cap[n]
            val k = c * tanh(tot / c) / tot
            h *= k
            s *= k
            height[n] = h
            spire[n] = s
        }
    }
}

/**
 * Finds, for each tower, how tall it may grow before it would come within reach of a tower
 * on the neighboring segment as the body bends. Towers on segments 0 and 1 have fixed caps
 * so they stay clear of the antlers and mane.
 */
object TowerClearance {
    /** Fills [towers].cap using each segment's up axis from [frames]. */
    fun solve(towers: Towers, frames: Array<Basis>) {
        val caps = towers.cap
        caps.fill(99f)
        for (i in 0 until DragonConfig.SEGMENTS - 1) {
            val ua = frames[i].u
            val ub = frames[i + 1].u
            val nx = ua.x - ub.x; val ny = ua.y - ub.y; val nz = ua.z - ub.z
            val qa = nx * nx + ny * ny + nz * nz
            for (ja in 0..2) {
                val a = 3 * i + ja
                for (jb in 0..2) {
                    val b = 3 * (i + 1) + jb
                    val dx = towers.baseX[a] - towers.baseX[b]
                    val dy = towers.baseY[a] - towers.baseY[b]
                    val dz = towers.baseZ[a] - towers.baseZ[b]
                    val g = 0.75f * (towers.width[a] + towers.width[b]) + 0.1f
                    val qb = 2f * (dx * nx + dy * ny + dz * nz)
                    val qc = dx * dx + dy * dy + dz * dz - g * g
                    var h: Float
                    if (qc <= 0f) {
                        h = 0.3f
                    } else {
                        val disc = qb * qb - 4f * qa * qc
                        h = if (qb < 0f && qa > 1e-7f && disc >= 0f) (-qb - sqrt(disc)) / (2f * qa) else 99f
                    }
                    h *= 0.9f
                    if (h < caps[a]) caps[a] = h
                    if (h < caps[b]) caps[b] = h
                }
            }
        }
        for (n in caps.indices) if (caps[n] < 0.3f) caps[n] = 0.3f
        for (j in 0..2) {
            caps[j] = minOf(caps[j], 1.1f)
            caps[3 + j] = minOf(caps[3 + j], 2.3f)
        }
    }
}
