package com.technomancer.neonlong.tools.assetgen

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

class GlbWriterTest {
    @Test
    fun blockIsAValidGlb() {
        val mesh = BodyParts.block()
        val bytes = GlbWriter.encode(mesh)
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0x46546C67, b.getInt(0), "magic glTF")
        assertEquals(2, b.getInt(4), "version")
        assertEquals(bytes.size, b.getInt(8))
        val jsonLen = b.getInt(12)
        val json = Json.parseToJsonElement(String(bytes, 20, jsonLen, Charsets.UTF_8)).jsonObject
        val prim = json["meshes"]!!.jsonArray[0].jsonObject["primitives"]!!.jsonArray[0].jsonObject
        assertNotNull(prim["attributes"]!!.jsonObject["NORMAL"])
        val accessors = json["accessors"]!!.jsonArray
        assertEquals(36, accessors[3].jsonObject["count"]!!.jsonPrimitive.int, "12 triangles")
        val pos = accessors[0].jsonObject
        for (i in 0..2) {
            assertEquals(-0.5f, pos["min"]!!.jsonArray[i].jsonPrimitive.float, 1e-6f)
            assertEquals(0.5f, pos["max"]!!.jsonArray[i].jsonPrimitive.float, 1e-6f)
        }
        assertEquals(12, mesh.triangleCount)
        assertEquals(24, mesh.vertexCount)
    }

    @Test
    fun deterministic() {
        assertArrayEquals(GlbWriter.encode(BodyParts.tower3Tier()), GlbWriter.encode(BodyParts.tower3Tier()))
        fun png(img: java.awt.image.BufferedImage) = ByteArrayOutputStream().also { ImageIO.write(img, "png", it) }.toByteArray()
        assertArrayEquals(png(TextureGen.facade()), png(TextureGen.facade()))
        assertArrayEquals(png(TextureGen.armour()), png(TextureGen.armour()))
    }
}
