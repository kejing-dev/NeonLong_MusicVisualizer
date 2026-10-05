package com.technomancer.neonlong.debug

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Debug switches, read once at startup. Only consulted in debug builds. */
object DebugFlags {
    /** Intent extra that starts the entity-count spike: `--ez nl.spike true`. */
    const val EXTRA_SPIKE = "nl.spike"

    /**
     * Reads a boolean system property (set with `adb shell setprop <name> 1`).
     * Uses the hidden SystemProperties class, so it's debug-only.
     */
    fun systemFlag(name: String): Boolean = try {
        val c = Class.forName("android.os.SystemProperties")
        val value = c.getMethod("get", String::class.java, String::class.java).invoke(null, name, "0") as String
        value == "1" || value.equals("true", ignoreCase = true)
    } catch (e: Exception) {
        false
    }

    /** True if the Activity hosting [context] was started with the given boolean extra. */
    fun intentFlag(context: Context, extra: String): Boolean {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is Activity) return c.intent?.getBooleanExtra(extra, false) == true
            c = c.baseContext
        }
        return false
    }
}
