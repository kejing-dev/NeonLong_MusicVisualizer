package com.technomancer.neonlong.tools.assetgen

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** More complex shapes: rotated boxes, cones, lathes, icospheres and tubes. */
object Shapes {
    /**
     * Box of size ([w], [h], [d]) centered at ([cx], [cy], [cz]), rotated by Euler angles
     * ([rx], [ry], [rz]) applied in X, then Y, then Z order.
     */
    fun orientedBox(
        m: MeshBuilder, cx: Float, cy: Float, cz: Float, w: Float, h: Float, d: Float,
        rx: Float = 0f, ry: Float = 0f, rz: Float = 0f,
    ) {
        val tmp = MeshBuilder("tmp")
        Primitives.box(tmp, -w / 2, -h / 2, -d / 2, w / 2, h / 2, d / 2)
        val r = eulerMatrix(rx, ry, rz)
        val base = m.vertexCount
        for (i in 0 until tmp.vertexCount) {
            val x = tmp.positions[i * 3]; val y = tmp.positions[i * 3 + 1]; val z = tmp.positions[i * 3 + 2]
            val nx = tmp.normals[i * 3]; val ny = tmp.normals[i * 3 + 1]; val nz = tmp.normals[i * 3 + 2]
            m.vertex(
                cx + r[0] * x + r[1] * y + r[2] * z, cy + r[3] * x + r[4] * y + r[5] * z, cz + r[6] * x + r[7] * y + r[8] * z,
                r[0] * nx + r[1] * ny + r[2] * nz, r[3] * nx + r[4] * ny + r[5] * nz, r[6] * nx + r[7] * ny + r[8] * nz,
                tmp.uvs[i * 2], tmp.uvs[i * 2 + 1],
            )
        }
        for (i in tmp.indices) m.indices += i + base
    }

    /** Row-major 3×3 rotation for Euler angles applied X first, then Y, then Z (R = Rz·Ry·Rx). */
    fun eulerMatrix(rx: Float, ry: Float, rz: Float): FloatArray {
        val cx = cos(rx); val sx = sin(rx); val cy = cos(ry); val sy = sin(ry); val cz = cos(rz); val sz = sin(rz)
        return floatArrayOf(
            cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx,
            sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx,
            -sy, cy * sx, cy * cx,
        )
    }

    /**
     * Base-anchored cone of radius 1 and height 1 with [sides] sides and [rings] rings, bent
     * forward by z += [bend]·y² (a claw), capped at the base.
     */
    fun cone(m: MeshBuilder, sides: Int, rings: Int, bend: Float = 0f) {
        val base = m.vertexCount
        for (r in 0..rings) {
            val y = r.toFloat() / rings
            val rad = 1f - y
            for (s in 0..sides) {
                val a = (2 * PI * s / sides).toFloat()
                val nx = cos(a); val nz = sin(a)
                m.vertex(nx * rad, y, nz * rad + bend * y * y, nx, 0.5f, nz, s.toFloat() / sides, y)
            }
        }
        val stride = sides + 1
        for (r in 0 until rings) for (s in 0 until sides) {
            val a = base + r * stride + s
            m.quad(a, a + stride, a + stride + 1, a + 1)
        }
        val c = m.vertex(0f, 0f, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
        for (s in 0 until sides) {
            val a0 = (2 * PI * s / sides).toFloat(); val a1 = (2 * PI * (s + 1) / sides).toFloat()
            val p0 = m.vertex(cos(a0), 0f, sin(a0), 0f, -1f, 0f, 0f, 0f)
            val p1 = m.vertex(cos(a1), 0f, sin(a1), 0f, -1f, 0f, 1f, 0f)
            m.triangle(c, p0, p1)
        }
    }

    /** Surface of revolution about Y through the (radius, height) [profile] points. */
    fun lathe(m: MeshBuilder, profile: List<Pair<Float, Float>>, segments: Int) {
        val base = m.vertexCount
        for (i in profile.indices) {
            val (pr, py) = profile[i]
            val (r0, y0) = profile[maxOf(0, i - 1)]
            val (r1, y1) = profile[minOf(profile.size - 1, i + 1)]
            val tr = r1 - r0; val ty = y1 - y0 // profile tangent; the normal is perpendicular
            for (s in 0..segments) {
                val a = (2 * PI * s / segments).toFloat()
                val c = cos(a); val sn = sin(a)
                m.vertex(pr * c, py, pr * sn, ty * c, -tr, ty * sn, s.toFloat() / segments, py)
            }
        }
        val stride = segments + 1
        for (i in 0 until profile.size - 1) for (s in 0 until segments) {
            val a = base + i * stride + s
            m.quad(a, a + stride, a + stride + 1, a + 1)
        }
    }

    /** Icosphere of [radius] subdivided [levels] times. */
    fun icosphere(m: MeshBuilder, radius: Float, levels: Int) {
        val t = ((1 + sqrt(5.0)) / 2).toFloat()
        val raw = listOf(
            floatArrayOf(-1f, t, 0f), floatArrayOf(1f, t, 0f), floatArrayOf(-1f, -t, 0f), floatArrayOf(1f, -t, 0f),
            floatArrayOf(0f, -1f, t), floatArrayOf(0f, 1f, t), floatArrayOf(0f, -1f, -t), floatArrayOf(0f, 1f, -t),
            floatArrayOf(t, 0f, -1f), floatArrayOf(t, 0f, 1f), floatArrayOf(-t, 0f, -1f), floatArrayOf(-t, 0f, 1f),
        )
        val verts = raw.map { unit(it[0], it[1], it[2]) }.toMutableList()
        var faces = listOf(
            intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7), intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
            intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
            intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
            intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10), intArrayOf(8, 6, 7), intArrayOf(9, 8, 1),
        )
        repeat(levels) {
            val cache = HashMap<Long, Int>()
            fun mid(a: Int, b: Int): Int {
                val key = if (a < b) (a.toLong() shl 32) or b.toLong() else (b.toLong() shl 32) or a.toLong()
                return cache.getOrPut(key) {
                    val va = verts[a]; val vb = verts[b]
                    verts += unit((va[0] + vb[0]) / 2, (va[1] + vb[1]) / 2, (va[2] + vb[2]) / 2)
                    verts.size - 1
                }
            }
            faces = faces.flatMap { f ->
                val ab = mid(f[0], f[1]); val bc = mid(f[1], f[2]); val ca = mid(f[2], f[0])
                listOf(intArrayOf(f[0], ab, ca), intArrayOf(f[1], bc, ab), intArrayOf(f[2], ca, bc), intArrayOf(ab, bc, ca))
            }
        }
        val base = m.vertexCount
        for (v in verts) m.vertex(v[0] * radius, v[1] * radius, v[2] * radius, v[0], v[1], v[2], 0.5f + v[0] / 2, 0.5f - v[1] / 2)
        for (f in faces) m.triangle(base + f[0], base + f[1], base + f[2])
    }

    private fun unit(x: Float, y: Float, z: Float): FloatArray {
        val l = sqrt(x * x + y * y + z * z)
        return floatArrayOf(x / l, y / l, z / l)
    }

    /**
     * Tube through [points] (x, y, z triples) with per-point [radii] and [sides] around,
     * using parallel-transport frames so it doesn't twist.
     */
    fun tube(m: MeshBuilder, points: List<FloatArray>, radii: FloatArray, sides: Int) {
        val n = points.size
        val base = m.vertexCount
        var nx = 0f; var ny = 0f; var nz = 0f
        for (i in 0 until n) {
            val p = points[i]
            val a = points[maxOf(0, i - 1)]; val b = points[minOf(n - 1, i + 1)]
            var tx = b[0] - a[0]; var ty = b[1] - a[1]; var tz = b[2] - a[2]
            val tl = sqrt(tx * tx + ty * ty + tz * tz); tx /= tl; ty /= tl; tz /= tl
            if (i == 0) {
                // Any vector not parallel to the tangent starts the frame.
                var rx = 0f; var ry = 1f; val rz = 0f
                if (abs(ty) > 0.9f) { rx = 1f; ry = 0f }
                nx = ry * tz - rz * ty; ny = rz * tx - rx * tz; nz = rx * ty - ry * tx
            } else {
                // Carry the previous normal along by removing its tangent component.
                val d = nx * tx + ny * ty + nz * tz
                nx -= d * tx; ny -= d * ty; nz -= d * tz
            }
            val nl = sqrt(nx * nx + ny * ny + nz * nz); nx /= nl; ny /= nl; nz /= nl
            val bx = ty * nz - tz * ny; val by = tz * nx - tx * nz; val bz = tx * ny - ty * nx
            for (s in 0..sides) {
                val ang = (2 * PI * s / sides).toFloat()
                val c = cos(ang); val sn = sin(ang)
                val ox = nx * c + bx * sn; val oy = ny * c + by * sn; val oz = nz * c + bz * sn
                m.vertex(p[0] + ox * radii[i], p[1] + oy * radii[i], p[2] + oz * radii[i], ox, oy, oz, s.toFloat() / sides, i.toFloat() / (n - 1))
            }
        }
        val stride = sides + 1
        for (i in 0 until n - 1) for (s in 0 until sides) {
            val a = base + i * stride + s
            m.quad(a, a + 1, a + stride + 1, a + stride)
        }
    }

    /** A base-anchored cone-like mesh stretched from [from] along unit [dir] by [len], radius [r]. */
    fun appendAlong(m: MeshBuilder, shape: MeshBuilder, from: FloatArray, dir: FloatArray, len: Float, r: Float) {
        // Basis with Y along dir.
        val y = dir
        var ax = 1f; var ay = 0f; var az = 0f
        if (abs(y[0]) > 0.9f) { ax = 0f; az = 1f }
        // x = normalize(a × y), z = x × y... keep right-handed: z = normalize(x × y) flipped as needed.
        var xx = ay * y[2] - az * y[1]; var xy = az * y[0] - ax * y[2]; var xz = ax * y[1] - ay * y[0]
        val xl = sqrt(xx * xx + xy * xy + xz * xz); xx /= xl; xy /= xl; xz /= xl
        val zx = xy * y[2] - xz * y[1]; val zy = xz * y[0] - xx * y[2]; val zz = xx * y[1] - xy * y[0]
        val base = m.vertexCount
        for (i in 0 until shape.vertexCount) {
            val px = shape.positions[i * 3] * r; val py = shape.positions[i * 3 + 1] * len; val pz = shape.positions[i * 3 + 2] * r
            val nx = shape.normals[i * 3]; val ny = shape.normals[i * 3 + 1]; val nz = shape.normals[i * 3 + 2]
            m.vertex(
                from[0] + xx * px + y[0] * py + zx * pz, from[1] + xy * px + y[1] * py + zy * pz, from[2] + xz * px + y[2] * py + zz * pz,
                xx * nx + y[0] * ny + zx * nz, xy * nx + y[1] * ny + zy * nz, xz * nx + y[2] * ny + zz * nz,
                shape.uvs[i * 2], shape.uvs[i * 2 + 1],
            )
        }
        for (i in shape.indices) m.indices += i + base
    }
}
