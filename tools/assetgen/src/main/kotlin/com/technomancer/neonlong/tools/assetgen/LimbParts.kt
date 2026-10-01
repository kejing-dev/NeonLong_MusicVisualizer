package com.technomancer.neonlong.tools.assetgen

/** Models for the mane, legs and joints. */
object LimbParts {
    /** Four-sided cone, base radius 1, height 1, base-anchored. */
    fun blade() = MeshBuilder("GEN_Blade").also { Shapes.cone(it, sides = 4, rings = 1) }

    /** Armored limb: a lathe through the profile below, 14 segments around, base-anchored. */
    fun limb() = MeshBuilder("GEN_Limb").also {
        Shapes.lathe(
            it,
            listOf(0f to 0f, 0.62f to 0.02f, 0.95f to 0.14f, 1.0f to 0.32f, 0.86f to 0.6f, 0.7f to 0.85f, 0.58f to 0.98f, 0f to 1f),
            segments = 14,
        )
    }

    /** Curved claw: cone of radius 1 and height 1, 8 × 4 segments, bent forward by z += 0.9·y². */
    fun talon() = MeshBuilder("GEN_Talon").also { Shapes.cone(it, sides = 8, rings = 4, bend = 0.9f) }

    /** Joint ring: torus with major radius 1, tube 0.12, 6 × 24, facing +Z. */
    fun torus() = MeshBuilder("GEN_Torus").also { Primitives.torusXY(it, 1f, 0.12f, radial = 6, tubular = 24) }
}

/** Models for the pearl. */
object PearlParts {
    fun core() = MeshBuilder("GEN_PearlCore").also { Primitives.sphere(it, 0.5f, 32, 20) }

    fun shell() = MeshBuilder("GEN_PearlShell").also { Shapes.icosphere(it, 0.72f, 2) }

    /** Thin ring of major radius 1 (scaled per ring at runtime), tube 0.018, 6 × 64. */
    fun ring() = MeshBuilder("GEN_PearlRing").also { Primitives.torusXY(it, 1f, 0.018f, radial = 6, tubular = 64) }
}
