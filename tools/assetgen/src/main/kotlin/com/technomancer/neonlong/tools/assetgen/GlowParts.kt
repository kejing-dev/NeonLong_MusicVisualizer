package com.technomancer.neonlong.tools.assetgen

/** Models for the faked glow. */
object GlowParts {
    /**
     * Three 1 × 1 cards crossed at the center, one per axis plane, each visible from both
     * sides. Drawn additively with GEN_Halo, they read as a soft ball of light from any angle.
     */
    fun haloCards() = MeshBuilder("GEN_HaloQuad").also { m ->
        val h = 0.5f
        // XY plane (facing ±Z), ZY plane (facing ±X), XZ plane (facing ±Y).
        card(m, floatArrayOf(-h, -h, 0f), floatArrayOf(h, -h, 0f), floatArrayOf(h, h, 0f), floatArrayOf(-h, h, 0f), 0f, 0f, 1f)
        card(m, floatArrayOf(0f, -h, h), floatArrayOf(0f, -h, -h), floatArrayOf(0f, h, -h), floatArrayOf(0f, h, h), 1f, 0f, 0f)
        card(m, floatArrayOf(-h, 0f, h), floatArrayOf(h, 0f, h), floatArrayOf(h, 0f, -h), floatArrayOf(-h, 0f, -h), 0f, 1f, 0f)
    }

    /** One card as two quads back to back, so it shows from both sides. */
    private fun card(m: MeshBuilder, a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray, nx: Float, ny: Float, nz: Float) {
        val a1 = m.vertex(a[0], a[1], a[2], nx, ny, nz, 0f, 1f)
        val b1 = m.vertex(b[0], b[1], b[2], nx, ny, nz, 1f, 1f)
        val c1 = m.vertex(c[0], c[1], c[2], nx, ny, nz, 1f, 0f)
        val d1 = m.vertex(d[0], d[1], d[2], nx, ny, nz, 0f, 0f)
        m.quad(a1, b1, c1, d1)
        val a2 = m.vertex(a[0], a[1], a[2], -nx, -ny, -nz, 0f, 1f)
        val b2 = m.vertex(b[0], b[1], b[2], -nx, -ny, -nz, 1f, 1f)
        val c2 = m.vertex(c[0], c[1], c[2], -nx, -ny, -nz, 1f, 0f)
        val d2 = m.vertex(d[0], d[1], d[2], -nx, -ny, -nz, 0f, 0f)
        m.quad(d2, c2, b2, a2)
    }
}
