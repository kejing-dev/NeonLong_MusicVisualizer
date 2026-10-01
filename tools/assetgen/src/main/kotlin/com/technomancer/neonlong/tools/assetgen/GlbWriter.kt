package com.technomancer.neonlong.tools.assetgen

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/** PBR factors written into a model's single material. The app overrides them at runtime. */
class MaterialFactors(
    val baseColor: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    val metallic: Float = 0f,
    val roughness: Float = 0.5f,
    val emissive: FloatArray = floatArrayOf(0f, 0f, 0f),
)

/**
 * Writes one mesh as a binary glTF 2.0 (.glb): positions, normals, UVs, 32-bit indices and
 * one material. Output is byte-for-byte deterministic for the same mesh.
 */
object GlbWriter {
    fun write(file: File, mesh: MeshBuilder, material: MaterialFactors = MaterialFactors()) {
        file.parentFile?.mkdirs()
        file.writeBytes(encode(mesh, material))
    }

    fun encode(mesh: MeshBuilder, material: MaterialFactors = MaterialFactors()): ByteArray {
        val vc = mesh.vertexCount
        val bin = ByteArrayOutputStream()
        fun floats(values: List<Float>) {
            val b = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            for (v in values) b.putFloat(v)
            bin.write(b.array())
        }
        floats(mesh.positions)
        val normalsOffset = bin.size()
        floats(mesh.normals)
        val uvOffset = bin.size()
        floats(mesh.uvs)
        val indexOffset = bin.size()
        val ib = ByteBuffer.allocate(mesh.indices.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in mesh.indices) ib.putInt(i)
        bin.write(ib.array())
        while (bin.size() % 4 != 0) bin.write(0)
        val binBytes = bin.toByteArray()

        fun f(v: Float) = String.format(Locale.ROOT, "%.6g", v)
        val min = "[${f(mesh.min(0))},${f(mesh.min(1))},${f(mesh.min(2))}]"
        val max = "[${f(mesh.max(0))},${f(mesh.max(1))},${f(mesh.max(2))}]"
        val bc = material.baseColor.joinToString(",") { f(it) }
        val em = material.emissive.joinToString(",") { f(it) }
        val json = """
            {"asset":{"version":"2.0","generator":"neonlong-assetgen"},
            "scene":0,"scenes":[{"nodes":[0]}],
            "nodes":[{"name":"${mesh.name}","mesh":0}],
            "meshes":[{"name":"${mesh.name}","primitives":[{"attributes":{"POSITION":0,"NORMAL":1,"TEXCOORD_0":2},"indices":3,"material":0}]}],
            "materials":[{"name":"${mesh.name}_mat","pbrMetallicRoughness":{"baseColorFactor":[$bc],"metallicFactor":${f(material.metallic)},"roughnessFactor":${f(material.roughness)}},"emissiveFactor":[$em]}],
            "buffers":[{"byteLength":${binBytes.size}}],
            "bufferViews":[
              {"buffer":0,"byteOffset":0,"byteLength":${vc * 12},"target":34962},
              {"buffer":0,"byteOffset":$normalsOffset,"byteLength":${vc * 12},"target":34962},
              {"buffer":0,"byteOffset":$uvOffset,"byteLength":${vc * 8},"target":34962},
              {"buffer":0,"byteOffset":$indexOffset,"byteLength":${mesh.indices.size * 4},"target":34963}],
            "accessors":[
              {"bufferView":0,"componentType":5126,"count":$vc,"type":"VEC3","min":$min,"max":$max},
              {"bufferView":1,"componentType":5126,"count":$vc,"type":"VEC3"},
              {"bufferView":2,"componentType":5126,"count":$vc,"type":"VEC2"},
              {"bufferView":3,"componentType":5125,"count":${mesh.indices.size},"type":"SCALAR"}]}
        """.trimIndent().replace("\n", "")
        var jsonBytes = json.toByteArray(Charsets.UTF_8)
        if (jsonBytes.size % 4 != 0) jsonBytes += ByteArray(4 - jsonBytes.size % 4) { ' '.code.toByte() }

        val total = 12 + 8 + jsonBytes.size + 8 + binBytes.size
        val out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        out.putInt(0x46546C67) // "glTF"
        out.putInt(2)
        out.putInt(total)
        out.putInt(jsonBytes.size); out.putInt(0x4E4F534A); out.put(jsonBytes) // JSON
        out.putInt(binBytes.size); out.putInt(0x004E4942); out.put(binBytes) // BIN
        return out.array()
    }
}
