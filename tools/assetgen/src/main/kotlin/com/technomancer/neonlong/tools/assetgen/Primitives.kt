package com.technomancer.neonlong.tools.assetgen

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Basic shapes, all Y up and 1 unit = 1 model unit. */
object Primitives {
    /**
     * Axis-aligned box from (x0, y0, z0) to (x1, y1, z1): 24 vertices (4 per face so each
     * face has flat normals) and UVs 0–1 per face. [v0], [v1] set the V range on side faces.
     */
    fun box(
        m: MeshBuilder,
        x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float,
        v0: Float = 0f, v1: Float = 1f,
    ) {
        // +X, −X, +Y, −Y, +Z, −Z; corners wound counter-clockwise seen from outside.
        face(m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, v0, v1)
        face(m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, v0, v1)
        face(m, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, 0f, 1f)
        face(m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, 0f, 1f)
        face(m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, v0, v1)
        face(m, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, v0, v1)
    }

    private fun face(
        m: MeshBuilder,
        ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float,
        cx: Float, cy: Float, cz: Float, dx: Float, dy: Float, dz: Float,
        nx: Float, ny: Float, nz: Float, v0: Float, v1: Float,
    ) {
        val a = m.vertex(ax, ay, az, nx, ny, nz, 0f, v0)
        val b = m.vertex(bx, by, bz, nx, ny, nz, 1f, v0)
        val c = m.vertex(cx, cy, cz, nx, ny, nz, 1f, v1)
        val d = m.vertex(dx, dy, dz, nx, ny, nz, 0f, v1)
        m.quad(a, b, c, d)
    }

    /** Capped cylinder of [radius] from y = 0 to y = 1 with [sides] sides. */
    fun cylinderBase(m: MeshBuilder, radius: Float, sides: Int) {
        for (i in 0 until sides) {
            val a0 = (2 * PI * i / sides).toFloat()
            val a1 = (2 * PI * (i + 1) / sides).toFloat()
            val x0 = cos(a0) * radius; val z0 = sin(a0) * radius
            val x1 = cos(a1) * radius; val z1 = sin(a1) * radius
            val u0 = i.toFloat() / sides; val u1 = (i + 1).toFloat() / sides
            val p = m.vertex(x0, 0f, z0, cos(a0), 0f, sin(a0), u0, 0f)
            val q = m.vertex(x1, 0f, z1, cos(a1), 0f, sin(a1), u1, 0f)
            val r = m.vertex(x1, 1f, z1, cos(a1), 0f, sin(a1), u1, 1f)
            val s = m.vertex(x0, 1f, z0, cos(a0), 0f, sin(a0), u0, 1f)
            m.quad(p, s, r, q)
            val topC = m.vertex(0f, 1f, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
            val t0 = m.vertex(x0, 1f, z0, 0f, 1f, 0f, 0.5f + x0 / 2, 0.5f + z0 / 2)
            val t1 = m.vertex(x1, 1f, z1, 0f, 1f, 0f, 0.5f + x1 / 2, 0.5f + z1 / 2)
            m.triangle(topC, t1, t0)
            val botC = m.vertex(0f, 0f, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
            val b0 = m.vertex(x0, 0f, z0, 0f, -1f, 0f, 0.5f + x0 / 2, 0.5f + z0 / 2)
            val b1 = m.vertex(x1, 0f, z1, 0f, -1f, 0f, 0.5f + x1 / 2, 0.5f + z1 / 2)
            m.triangle(botC, b0, b1)
        }
    }

    /** UV sphere of [radius] with [segs] around and [rings] top to bottom. */
    fun sphere(m: MeshBuilder, radius: Float, segs: Int, rings: Int) {
        val base = m.vertexCount
        for (r in 0..rings) {
            val phi = (PI * r / rings).toFloat()
            for (s in 0..segs) {
                val th = (2 * PI * s / segs).toFloat()
                val nx = sin(phi) * cos(th); val ny = cos(phi); val nz = sin(phi) * sin(th)
                m.vertex(nx * radius, ny * radius, nz * radius, nx, ny, nz, s.toFloat() / segs, r.toFloat() / rings)
            }
        }
        val stride = segs + 1
        for (r in 0 until rings) for (s in 0 until segs) {
            val a = base + r * stride + s
            val b = a + stride
            if (r != 0) m.triangle(a, a + 1, b)
            if (r != rings - 1) m.triangle(a + 1, b + 1, b)
        }
    }

    /**
     * Torus in the XY plane (facing +Z) with [major] and [tube] radii, [radial] segments
     * around the tube and [tubular] around the ring, rotated [rollRad] about Z.
     */
    fun torusXY(m: MeshBuilder, major: Float, tube: Float, radial: Int, tubular: Int, rollRad: Float = 0f) {
        val base = m.vertexCount
        for (j in 0..radial) {
            val v = (2 * PI * j / radial).toFloat()
            for (i in 0..tubular) {
                val u = (2 * PI * i / tubular).toFloat() + rollRad
                val cx = major * cos(u); val cy = major * sin(u)
                val nx = cos(v) * cos(u); val ny = cos(v) * sin(u); val nz = sin(v)
                m.vertex(cx + tube * nx, cy + tube * ny, tube * nz, nx, ny, nz, i.toFloat() / tubular, j.toFloat() / radial)
            }
        }
        val stride = tubular + 1
        for (j in 0 until radial) for (i in 0 until tubular) {
            val a = base + j * stride + i
            val b = a + stride
            m.quad(a, a + 1, b + 1, b)
        }
    }
}
