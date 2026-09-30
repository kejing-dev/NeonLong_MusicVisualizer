package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConfigCodecTest {
    @Serializable
    data class Sample(val speed: Float = 0.19f, val count: Int = 27, val name: String = "dragon")

    @Test
    fun unknownKeysIgnored() {
        val decoded = ConfigCodec.decode(Sample.serializer(), """{"notAField": 42, "alsoUnknown": {"x": 1}}""")
        assertEquals(Sample(), decoded)
    }

    @Test
    fun missingKeysUseDefaults() {
        val decoded = ConfigCodec.decode(Sample.serializer(), """{"count": 3}""")
        assertEquals(Sample(count = 3), decoded)
    }

    @Test
    fun roundTrip() {
        val value = Sample(speed = 0.5f, count = 9, name = "x")
        assertEquals(value, ConfigCodec.decode(Sample.serializer(), ConfigCodec.encode(Sample.serializer(), value)))
    }
}
