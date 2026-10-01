package com.technomancer.neonlong.tools.assetgen

import com.technomancer.neonlong.logic.dragon.HeadZones
import java.io.File

/** Collects triangle and texture numbers and writes them as a Markdown report. */
class Report {
    private val models = ArrayList<Triple<String, Int, Int>>()
    private val textures = ArrayList<Triple<String, Int, Int>>()
    private val notes = ArrayList<String>()

    fun model(mesh: MeshBuilder) {
        models += Triple(mesh.name, mesh.triangleCount, mesh.vertexCount)
    }

    fun texture(name: String, w: Int, h: Int) {
        textures += Triple(name, w, h)
    }

    fun note(text: String) {
        notes += text
    }

    fun triangles(name: String): Int = models.first { it.first == name }.second

    /** Triangles drawn by the whole dragon at once: model triangles × instances drawn. */
    fun dragonTriangles(): Int = INSTANCES.entries.sumOf { (name, count) ->
        (models.firstOrNull { it.first == name }?.second ?: 0) * count
    }

    fun write(file: File) {
        file.parentFile?.mkdirs()
        file.writeText(buildString {
            append("# Neon Lóng — generated asset report\n\n")
            append("Written by `:tools:assetgen`.\n\n")
            append("| Model | Triangles | Vertices | Instances | Triangles drawn |\n|---|---|---|---|---|\n")
            for ((n, t, v) in models) {
                val inst = INSTANCES[n] ?: 0
                append("| $n | $t | $v | $inst | ${t * inst} |\n")
            }
            append("\n**Dragon total: ${dragonTriangles()} triangles** (budget 200,000; tubes drawn as chains of GEN_CylBase pieces are included).\n")
            append("\n| Texture | Size |\n|---|---|\n")
            for ((n, w, h) in textures) append("| $n | $w × $h |\n")
            if (notes.isNotEmpty()) {
                append("\n## Notes\n")
                for (n in notes) append("- $n\n")
            }
        })
    }

    companion object {
        /** How many of each model the dragon draws (body, head, limbs, pearl, tube pieces, breath). */
        val INSTANCES = mapOf(
            // 27 dorsal + 27 tier + 27 belly + 162 side plates + 36 belly lights + 81 caps + 60 whiskers + 4 palms
            // + 4 eyes/bezels + 160 breath particles
            "GEN_Block" to 27 * 3 + 162 + 36 + 81 + 60 + 4 + 4 + 160,
            "GEN_Tower3Tier" to 81,
            // 81 spires + 104 pistons + tube pieces (80 core + 80 tufts + 108 tail + 56 pearl flames + 24 cables)
            "GEN_CylBase" to 81 + 104 + 80 + 80 + 108 + 56 + 24,
            // 81 beacons + 8 joint balls + mouth glow
            "GEN_Ball" to 81 + 8 + 1,
            "GEN_SquareRing" to 27,
            // one model per head color zone
            *Array(HeadZones.COUNT) { "GEN_Head_$it" to 1 },
            "GEN_Jaw" to 1,
            "GEN_Antlers" to 1,
            "GEN_GlyphPlate" to 1,
            "GEN_Blade" to 24,
            // 8 limb pieces + 16 toes
            "GEN_Limb" to 24,
            "GEN_Talon" to 16,
            "GEN_Torus" to 8,
            "GEN_PearlCore" to 1,
            "GEN_PearlShell" to 1,
            "GEN_PearlRing" to 3,
        )
    }
}
