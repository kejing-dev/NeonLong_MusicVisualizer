package com.technomancer.neonlong.config

import android.content.Context
import android.util.Log
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.logic.config.ConfigCodec
import kotlinx.serialization.KSerializer
import java.io.FileNotFoundException

/**
 * Reads `assets/config/<name>.json` into a logic config class. A missing file
 * yields [defaults]; unknown keys are ignored and missing keys keep their defaults.
 */
object ConfigLoader {
    fun <T> load(context: Context, name: String, serializer: KSerializer<T>, defaults: T): T {
        val path = "config/$name.json"
        return try {
            val text = context.assets.open(path).bufferedReader().use { it.readText() }
            ConfigCodec.decode(serializer, text)
        } catch (e: FileNotFoundException) {
            defaults
        } catch (e: Exception) {
            Log.w(LOG_TAG, "[boot] config $path unreadable, using defaults: ${e.message}")
            defaults
        }
    }
}
