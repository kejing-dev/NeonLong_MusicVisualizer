package com.technomancer.neonlong.tools.assetgen

import kotlin.math.sqrt

/** Growable triangle mesh with per-vertex position, normal and UV. */
class MeshBuilder(val name: String) {
    val positions = ArrayList<Float>()
    val normals = ArrayList<Float>()
    val uvs = ArrayList<Float>()
    val indices = ArrayList<Int>()

    val vertexCount: Int get() = positions.size / 3
    val triangleCount: Int get() = indices.size / 3

    fun vertex(px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float): Int {
        positions += px; positions += py; positions += pz
        val l = sqrt(nx * nx + ny * ny + nz * nz).let { if (it < 1e-9f) 1f else it }
        normals += nx / l; normals += ny / l; normals += nz / l
        uvs += u; uvs += v
        return vertexCount - 1
    }

    fun triangle(a: Int, b: Int, c: Int) {
        indices += a; indices += b; indices += c
    }

    fun quad(a: Int, b: Int, c: Int, d: Int) {
        triangle(a, b, c)
        triangle(a, c, d)
    }

    /** Appends [other], offsetting its indices. */
    fun append(other: MeshBuilder) {
        val base = vertexCount
        positions.addAll(other.positions)
        normals.addAll(other.normals)
        uvs.addAll(other.uvs)
        for (i in other.indices) indices += i + base
    }

    fun min(axis: Int): Float = (axis until positions.size step 3).minOf { positions[it] }
    fun max(axis: Int): Float = (axis until positions.size step 3).maxOf { positions[it] }
}
