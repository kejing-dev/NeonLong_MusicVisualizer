package com.technomancer.neonlong.logic.config

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * JSON codec for the config data classes.
 * Unknown keys are ignored and missing keys fall back to the data class defaults.
 */
object ConfigCodec {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun <T> decode(serializer: KSerializer<T>, text: String): T = json.decodeFromString(serializer, text)

    fun <T> encode(serializer: KSerializer<T>, value: T): String = json.encodeToString(serializer, value)
}
