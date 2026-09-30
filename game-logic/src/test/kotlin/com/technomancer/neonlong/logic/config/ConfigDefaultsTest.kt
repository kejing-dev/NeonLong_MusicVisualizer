package com.technomancer.neonlong.logic.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** The shipped JSON overrides must parse and match the code defaults exactly. */
class ConfigDefaultsTest {
    private val dir = File("../app/src/main/assets/config")

    private fun read(name: String): String {
        val f = File(dir, "$name.json")
        assertTrue(f.isFile, "missing ${f.absolutePath}")
        return f.readText()
    }

    @Test
    fun dragon() = assertEquals(DragonConfig(), ConfigCodec.decode(DragonConfig.serializer(), read("dragon")))

    @Test
    fun scene() = assertEquals(SceneConfig(), ConfigCodec.decode(SceneConfig.serializer(), read("scene")))

    @Test
    fun comfort() = assertEquals(ComfortConfig(), ConfigCodec.decode(ComfortConfig.serializer(), read("comfort")))
}
