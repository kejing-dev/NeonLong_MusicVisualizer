package com.technomancer.neonlong.tools.assetgen

import java.io.File

/** Material factors for the body's metal blocks and towers; the app sets glow at runtime. */
private val BLOCK = MaterialFactors(floatArrayOf(0.063f, 0.055f, 0.094f, 1f), metallic = 0.75f, roughness = 0.33f)
private val TOWER = MaterialFactors(floatArrayOf(0.055f, 0.051f, 0.086f, 1f), metallic = 0.7f, roughness = 0.35f)
private val CHROME = MaterialFactors(floatArrayOf(0.604f, 0.604f, 0.651f, 1f), metallic = 1f, roughness = 0.18f)
private val NEON = MaterialFactors(floatArrayOf(0f, 0f, 0f, 1f), metallic = 0f, roughness = 1f, emissive = floatArrayOf(1f, 1f, 1f))

/**
 * Writes every generated model into `<assets>/models`, every texture into `<assets>/textures`,
 * and a triangle/texture report to [args] [1].
 * Args: <assets dir> <report file>.
 */
fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    require(args.size == 2) { "usage: assetgen <assetsDir> <reportFile>" }
    val assets = File(args[0])
    val report = Report()

    val models = listOf(
        BodyParts.block() to BLOCK,
        BodyParts.tower3Tier() to TOWER,
        BodyParts.cylBase() to CHROME,
        BodyParts.ball() to NEON,
        BodyParts.squareRing() to NEON,
    )
    for ((mesh, mat) in models) {
        GlbWriter.write(File(assets, "models/${mesh.name}.glb"), mesh, mat)
        report.model(mesh)
    }

    val textures = listOf(
        "GEN_Facade" to TextureGen.facade(),
        "GEN_Armour" to TextureGen.armour(),
        "GEN_ArmourBump" to TextureGen.armour(bump = true),
        "GEN_Glow" to TextureGen.glow(),
        "GEN_EnvNightTemp" to TextureGen.nightEnvironment(),
    )
    for ((name, img) in textures) {
        TextureGen.writePng(img, File(assets, "textures/$name.png"))
        report.texture(name, img.width, img.height)
    }

    report.write(File(args[1]))
    println("assetgen: ${models.size} models, ${textures.size} textures")
}
