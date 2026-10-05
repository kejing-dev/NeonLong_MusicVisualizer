package com.technomancer.neonlong.tools.assetgen

import java.io.File

/** Material factors for the body's metal blocks and towers; the app sets glow at runtime. */
private val BLOCK = MaterialFactors(floatArrayOf(0.063f, 0.055f, 0.094f, 1f), metallic = 0.75f, roughness = 0.33f)
private val TOWER = MaterialFactors(floatArrayOf(0.055f, 0.051f, 0.086f, 1f), metallic = 0.7f, roughness = 0.35f)
private val ARMOUR = MaterialFactors(floatArrayOf(0.082f, 0.075f, 0.114f, 1f), metallic = 0.85f, roughness = 0.32f)
private val CHROME = MaterialFactors(floatArrayOf(0.604f, 0.604f, 0.651f, 1f), metallic = 1f, roughness = 0.18f)
private val NEON = MaterialFactors(floatArrayOf(0f, 0f, 0f, 1f), metallic = 0f, roughness = 1f, emissive = floatArrayOf(1f, 1f, 1f))

/** Every generated model with the material factors written into it. */
fun allModels(): List<Pair<MeshBuilder, MaterialFactors>> = HeadParts.headZones().map { it to BLOCK } + listOf(
    BodyParts.block() to BLOCK,
    BodyParts.tower3Tier() to TOWER,
    BodyParts.cylBase() to CHROME,
    BodyParts.ball() to NEON,
    BodyParts.squareRing() to NEON,
    HeadParts.jaw() to BLOCK,
    HeadParts.antlers() to NEON,
    HeadParts.glyphPlate() to NEON,
    GlowParts.haloCards() to NEON,
    LimbParts.blade() to NEON,
    LimbParts.limb() to ARMOUR,
    LimbParts.talon() to NEON,
    LimbParts.torus() to NEON,
    PearlParts.core() to NEON,
    PearlParts.shell() to CHROME,
    PearlParts.ring() to NEON,
)

/**
 * Writes every generated model into `<assets>/models`, every texture into `<assets>/textures`,
 * and a triangle/texture report.
 * Args: <assets dir> <report file> <glyph font file>.
 */
fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    require(args.size == 3) { "usage: assetgen <assetsDir> <reportFile> <fontFile>" }
    val assets = File(args[0])
    val report = Report()

    val models = allModels()
    for ((mesh, mat) in models) {
        GlbWriter.write(File(assets, "models/${mesh.name}.glb"), mesh, mat)
        report.model(mesh)
    }

    val textures = listOf(
        "GEN_Facade" to TextureGen.facade(),
        "GEN_Armour" to TextureGen.armour(),
        "GEN_ArmourBump" to TextureGen.armour(bump = true),
        "GEN_Glow" to TextureGen.glow(),
        "GEN_BodySkin" to TextureGen.bodySkin(),
        "GEN_Halo" to TextureGen.halo(),
        "GEN_IBL_Night" to TextureGen.nightEnvironment(),
        "GEN_Glyph_Long" to TextureGen.glyph(File(args[2])) { report.note(it) },
    )
    for ((name, img) in textures) {
        TextureGen.writePng(img, File(assets, "textures/$name.png"))
        report.texture(name, img.width, img.height)
    }

    report.write(File(args[1]))
    println("assetgen: ${models.size} models, ${textures.size} textures, dragon ≈ ${report.dragonTriangles()} triangles")
}
