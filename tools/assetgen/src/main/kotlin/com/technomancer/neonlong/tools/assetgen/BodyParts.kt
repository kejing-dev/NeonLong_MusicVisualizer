package com.technomancer.neonlong.tools.assetgen

import kotlin.math.PI

/** Models for the dragon's body: blocks, towers, rods, balls and the square neon frame. */
object BodyParts {
    /** Unit box centered at the origin. */
    fun block() = MeshBuilder("GEN_Block").also {
        Primitives.box(it, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f)
    }

    /**
     * Three stacked boxes, base-anchored: y ∈ [0, 0.55], [0.55, 0.83], [0.83, 1.0] with widths
     * 1.0, 0.72, 0.46. Each tier's side UVs span its own height.
     */
    fun tower3Tier() = MeshBuilder("GEN_Tower3Tier").also { m ->
        val tiers = arrayOf(floatArrayOf(0f, 0.55f, 1.0f), floatArrayOf(0.55f, 0.83f, 0.72f), floatArrayOf(0.83f, 1.0f, 0.46f))
        for (t in tiers) {
            val h = t[2] / 2f
            Primitives.box(m, -h, t[0], -h, h, t[1], h, v0 = 0f, v1 = 1f)
        }
    }

    /** Base-anchored capped cylinder, radius 1, 10 sides. */
    fun cylBase() = MeshBuilder("GEN_CylBase").also { Primitives.cylinderBase(it, 1f, 10) }

    /** Sphere of radius 1, 20 × 14. */
    fun ball() = MeshBuilder("GEN_Ball").also { Primitives.sphere(it, 1f, 20, 14) }

    /** Thin torus with 4 × 4 segments turned 45° about Z, so it reads as a square frame. */
    fun squareRing() = MeshBuilder("GEN_SquareRing").also {
        Primitives.torusXY(it, 1f, 0.03f, radial = 4, tubular = 4, rollRad = (PI / 4).toFloat())
    }
}
