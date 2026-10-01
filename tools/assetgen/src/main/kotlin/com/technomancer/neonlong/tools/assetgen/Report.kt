package com.technomancer.neonlong.tools.assetgen

import java.io.File

/** Collects triangle and texture numbers and writes them as a Markdown report. */
class Report {
    private val models = ArrayList<Triple<String, Int, Int>>()
    private val textures = ArrayList<Triple<String, Int, Int>>()

    fun model(mesh: MeshBuilder) {
        models += Triple(mesh.name, mesh.triangleCount, mesh.vertexCount)
    }

    fun texture(name: String, w: Int, h: Int) {
        textures += Triple(name, w, h)
    }

    fun triangles(name: String): Int = models.first { it.first == name }.second

    fun write(file: File) {
        file.parentFile?.mkdirs()
        file.writeText(buildString {
            append("# Neon Lóng — generated asset report\n\n")
            append("Written by `:tools:assetgen`. Per-model counts; the per-frame totals depend on instance counts.\n\n")
            append("| Model | Triangles | Vertices |\n|---|---|---|\n")
            for ((n, t, v) in models) append("| $n | $t | $v |\n")
            append("\n| Texture | Size |\n|---|---|\n")
            for ((n, w, h) in textures) append("| $n | $w × $h |\n")
        })
    }
}
