package com.technomancer.neonlong.tools.assetgen

import com.technomancer.neonlong.logic.dragon.HeadZones
import com.technomancer.neonlong.logic.math.CatmullRom
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.pow
import kotlin.math.sqrt

/** The head's models, in head space (x side, y up, z forward; unscaled). */
object HeadParts {
    /** Head blocks: x, y, z, w, h, d, rx, ry, rz[, glow]. Glow defaults to 1. Rows with x ≠ 0 are mirrored. */
    private val BLOCKS = arrayOf(
        floatArrayOf(0f, 0.25f, -0.2f, 2.2f, 1.4f, 2.0f, 0f, 0f, 0f), // cranium
        floatArrayOf(0f, 1.05f, -0.35f, 1.6f, 0.35f, 1.6f, 0f, 0f, 0f), // skull setback
        floatArrayOf(0f, 1.33f, -0.5f, 1.0f, 0.25f, 1.1f, 0f, 0f, 0f), // crown
        floatArrayOf(0f, 0.3f, -1.35f, 1.7f, 1.1f, 0.5f, 0f, 0f, 0f), // occiput
        floatArrayOf(0f, 0.1f, -1.8f, 1.45f, 0.95f, 0.6f, 0f, 0f, 0f, 0.7f), // neck collar
        floatArrayOf(0f, 0.1f, 1.45f, 1.75f, 0.95f, 1.2f, 0f, 0f, 0f), // snout tier 1
        floatArrayOf(0f, 0.05f, 2.45f, 1.45f, 0.8f, 1.0f, 0f, 0f, 0f), // snout tier 2
        floatArrayOf(0f, 0.0f, 3.25f, 1.15f, 0.66f, 0.8f, 0f, 0f, 0f), // snout tier 3
        floatArrayOf(0f, 0.05f, 3.8f, 0.95f, 0.5f, 0.45f, 0f, 0f, 0f), // snout tip
        floatArrayOf(0f, 0.68f, 1.5f, 0.9f, 0.25f, 1.1f, 0f, 0f, 0f), // ridge 1
        floatArrayOf(0f, 0.53f, 2.5f, 0.6f, 0.2f, 0.9f, 0f, 0f, 0f), // ridge 2
        floatArrayOf(0f, 0.42f, 3.25f, 0.36f, 0.16f, 0.7f, 0f, 0f, 0f), // ridge 3
        floatArrayOf(0.3f, 0.4f, 3.62f, 0.3f, 0.24f, 0.36f, 0f, 0f, 0f, 1.4f), // nostril
        floatArrayOf(0.62f, 1.02f, 0.72f, 0.95f, 0.22f, 1.05f, -0.12f, 0.08f, -0.32f), // brow slab
        floatArrayOf(0.9f, 1.12f, 0.1f, 0.5f, 0.18f, 0.7f, 0f, 0.1f, -0.5f), // brow slab 2
        floatArrayOf(1.2f, -0.05f, 0.1f, 0.35f, 0.9f, 1.6f, 0f, 0.25f, 0f), // cheek plate
        floatArrayOf(1.38f, 0.3f, -0.95f, 0.18f, 0.75f, 1.15f, 0f, 0.45f, 0.3f), // cheek fin
        floatArrayOf(1.3f, 0.75f, -1.25f, 0.14f, 0.55f, 0.9f, 0f, 0.55f, 0.55f), // cheek fin 2
    )

    /** Jaw blocks in pivot space: x, y, z, w, h, d. */
    private val JAW = arrayOf(
        floatArrayOf(0f, -0.22f, 1.0f, 1.9f, 0.4f, 2.2f),
        floatArrayOf(0f, -0.2f, 2.55f, 1.35f, 0.34f, 1.3f),
        floatArrayOf(0f, -0.18f, 3.45f, 1.0f, 0.3f, 0.9f),
        floatArrayOf(0f, -0.47f, 2.0f, 0.9f, 0.2f, 1.6f),
        floatArrayOf(0f, -0.6f, 2.2f, 0.45f, 0.14f, 1.0f),
    )

    /** Number of head blocks (center rows once, side rows twice). */
    val headBlockCount: Int get() = BLOCKS.sumOf { (if (it[0] == 0f) 1 else 2).toInt() }
    val jawBlockCount: Int get() = JAW.size

    private fun tooth(m: MeshBuilder, x: Float, y: Float, z: Float, len: Float, up: Boolean) {
        val d = floatArrayOf(0f, if (up) 1f else -1f, 0.15f)
        val l = sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2])
        for (i in 0..2) d[i] /= l
        val talon = MeshBuilder("t").also { Shapes.cone(it, 8, 4, bend = 0.9f) }
        Shapes.appendAlong(m, talon, floatArrayOf(x, y, z), d, len, 0.06f)
    }

    /**
     * The 24 head blocks, upper teeth and dark pupils, split into one model per color zone
     * (GEN_Head_0 … GEN_Head_8) so each zone can glow its own color.
     */
    fun headZones(): List<MeshBuilder> {
        val zones = List(HeadZones.COUNT) { MeshBuilder("GEN_Head_$it") }
        var i = 0
        for (b in BLOCKS) {
            val glow = if (b.size > 9) b[9] else 1f
            Shapes.orientedBox(zones[HeadZones.of(i++, glow)], b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7], b[8])
            if (b[0] != 0f) {
                Shapes.orientedBox(zones[HeadZones.of(i++, glow)], -b[0], b[1], b[2], b[3], b[4], b[5], b[6], -b[7], -b[8])
            }
        }
        val m = zones[HeadZones.DETAIL]
        for (s in floatArrayOf(-1f, 1f)) for (k in 0..6) {
            val z = 1.1f + 0.38f * k
            val half = if (z < 1.95f) 0.875f else if (z < 2.95f) 0.725f else 0.575f
            val y = if (z < 1.95f) -0.375f else if (z < 2.95f) -0.35f else -0.33f
            val len = if (k == 4) 0.42f else 0.18f + 0.05f * (k % 2)
            tooth(m, s * (half - 0.1f), y, z, len, up = false)
        }
        for (s in floatArrayOf(-1f, 1f)) Shapes.orientedBox(m, 1.16f * s, 0.56f, 0.58f, 0.03f, 0.19f, 0.06f)
        return zones
    }

    /** Five jaw blocks and lower teeth, in jaw-pivot space. */
    fun jaw() = MeshBuilder("GEN_Jaw").also { m ->
        for (b in JAW) Shapes.orientedBox(m, b[0], b[1], b[2], b[3], b[4], b[5])
        for (s in floatArrayOf(-1f, 1f)) for (k in 0..6) {
            val z = 1.2f + 0.36f * k
            val jhalf = if (z < 1.9f) 0.95f else if (z < 3.0f) 0.675f else 0.5f
            val len = if (k == 5) 0.36f else 0.15f + 0.04f * (k % 2)
            tooth(m, s * (jhalf - 0.1f), -0.02f, z, len, up = true)
        }
    }

    /** Main beams and three tines per side, as tapered tubes through smooth curves. */
    fun antlers() = MeshBuilder("GEN_Antlers").also { m ->
        val beam = arrayOf(
            floatArrayOf(0.4f, 1.35f, -0.55f), floatArrayOf(0.7f, 1.95f, -1.2f), floatArrayOf(0.95f, 2.55f, -2.2f),
            floatArrayOf(0.95f, 2.95f, -3.3f), floatArrayOf(0.75f, 3.15f, -4.3f),
        )
        val tines = arrayOf(
            arrayOf(floatArrayOf(0.72f, 2.0f, -1.3f), floatArrayOf(1.2f, 2.5f, -1.1f), floatArrayOf(1.5f, 3.0f, -1.4f)),
            arrayOf(floatArrayOf(0.96f, 2.7f, -2.6f), floatArrayOf(1.08f, 3.4f, -2.35f), floatArrayOf(1.02f, 3.8f, -2.6f)),
            arrayOf(floatArrayOf(0.9f, 3.02f, -3.6f), floatArrayOf(1.33f, 3.35f, -3.8f), floatArrayOf(1.58f, 3.5f, -4.3f)),
        )
        for (s in floatArrayOf(-1f, 1f)) {
            taperedCurve(m, beam, s, segs = 48, sides = 10, r0 = 0.15f, r1 = 0.025f)
            for (t in tines) taperedCurve(m, t, s, segs = 20, sides = 8, r0 = 0.065f, r1 = 0.015f)
        }
    }

    /** Tube along a centripetal Catmull-Rom through [ctrl] (x mirrored by [side]), radius r0 → r1. */
    private fun taperedCurve(m: MeshBuilder, ctrl: Array<FloatArray>, side: Float, segs: Int, sides: Int, r0: Float, r1: Float) {
        val pts = ctrl.map { Vec3(it[0] * side, it[1], it[2]) }
        // Extend the ends so the curve passes through the first and last points.
        val ext = ArrayList<Vec3>()
        ext += Vec3().set(pts[0]).scale(2f).sub(pts[1])
        ext += pts
        ext += Vec3().set(pts.last()).scale(2f).sub(pts[pts.size - 2])
        // Dense samples, then resampled to equal arc length.
        val dense = ArrayList<FloatArray>()
        val out = Vec3()
        for (i in 1 until ext.size - 2) {
            for (k in 0 until 32) {
                CatmullRom.eval(ext[i - 1], ext[i], ext[i + 1], ext[i + 2], k / 32f, out)
                dense += floatArrayOf(out.x, out.y, out.z)
            }
        }
        dense += floatArrayOf(pts.last().x, pts.last().y, pts.last().z)
        val cum = FloatArray(dense.size)
        for (i in 1 until dense.size) {
            val a = dense[i - 1]; val b = dense[i]
            cum[i] = cum[i - 1] + sqrt((b[0] - a[0]).pow(2) + (b[1] - a[1]).pow(2) + (b[2] - a[2]).pow(2))
        }
        val total = cum.last()
        val samples = ArrayList<FloatArray>()
        var j = 0
        for (i in 0..segs) {
            val target = total * i / segs
            while (j < dense.size - 2 && cum[j + 1] < target) j++
            val span = (cum[j + 1] - cum[j]).coerceAtLeast(1e-6f)
            val f = ((target - cum[j]) / span).coerceIn(0f, 1f)
            val a = dense[j]; val b = dense[j + 1]
            samples += floatArrayOf(a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f)
        }
        val radii = FloatArray(segs + 1) { i -> r0 + (r1 - r0) * (i.toFloat() / segs).pow(0.9f) }
        Shapes.tube(m, samples, radii, sides)
    }

    /** A 1 × 1 quad in the XY plane facing +Z, for the 龍 plate (scaled to 0.75 at runtime). */
    fun glyphPlate() = MeshBuilder("GEN_GlyphPlate").also { m ->
        val a = m.vertex(-0.5f, -0.5f, 0f, 0f, 0f, 1f, 0f, 1f)
        val b = m.vertex(0.5f, -0.5f, 0f, 0f, 0f, 1f, 1f, 1f)
        val c = m.vertex(0.5f, 0.5f, 0f, 0f, 0f, 1f, 1f, 0f)
        val d = m.vertex(-0.5f, 0.5f, 0f, 0f, 0f, 1f, 0f, 0f)
        m.quad(a, b, c, d)
    }
}
