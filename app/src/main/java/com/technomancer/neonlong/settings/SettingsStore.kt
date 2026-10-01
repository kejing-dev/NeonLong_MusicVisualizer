package com.technomancer.neonlong.settings

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.logic.state.Settings
import com.technomancer.neonlong.logic.state.SettingsModel
import com.technomancer.neonlong.logic.state.ViewMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The user's preferences, saved with DataStore. [state] always holds the current, valid
 * settings; the panel changes them with [update], which saves in the background.
 * Unknown keys are ignored and missing keys keep their defaults.
 */
class SettingsStore(
    context: Context,
    private val paletteCount: Int,
    private val minWidth: Float,
    private val maxWidth: Float,
) {
    private val store = context.applicationContext.settingsDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutable = MutableStateFlow(Settings())

    val state: StateFlow<Settings> = mutable

    /** The current settings; safe to read from any thread. */
    val current: Settings get() = mutable.value

    /** Reads the saved settings. Blocks briefly; called once at startup, before the first frame. */
    fun loadBlocking() {
        val loaded = try {
            read(runBlocking { store.data.first() })
        } catch (e: Exception) {
            Log.w(LOG_TAG, "[settings] couldn't read saved settings, using defaults: ${e.message}")
            Settings()
        }
        mutable.value = sanitize(loaded)
        Log.i(LOG_TAG, "[settings] loaded $current")
    }

    /** Applies [change] and saves the result if anything changed. */
    fun update(change: (Settings) -> Settings) {
        val before = mutable.value
        val next = sanitize(change(before))
        if (next == before) return
        mutable.value = next
        scope.launch {
            try {
                store.edit { write(it, next) }
            } catch (e: Exception) {
                Log.w(LOG_TAG, "[settings] couldn't save settings: ${e.message}")
            }
        }
    }

    private fun sanitize(s: Settings) = SettingsModel.sanitize(s, paletteCount, minWidth, maxWidth)

    private fun read(p: Preferences): Settings {
        val d = Settings()
        return Settings(
            palette = p[PALETTE] ?: d.palette,
            sensitivity = p[SENSITIVITY] ?: d.sensitivity,
            view = p[VIEW]?.let { name -> ViewMode.entries.firstOrNull { it.name == name } } ?: d.view,
            reduceMotion = p[REDUCE_MOTION] ?: d.reduceMotion,
            reduceFlashing = p[REDUCE_FLASHING] ?: d.reduceFlashing,
            seated = p[SEATED] ?: d.seated,
            spectrumPanel = p[SPECTRUM_PANEL] ?: d.spectrumPanel,
            uiSounds = p[UI_SOUNDS] ?: d.uiSounds,
            volumeWidth = p[VOLUME_WIDTH] ?: d.volumeWidth,
            safetySeen = p[SAFETY_SEEN] ?: d.safetySeen,
        )
    }

    private fun write(p: androidx.datastore.preferences.core.MutablePreferences, s: Settings) {
        p[SCHEMA] = SettingsModel.SCHEMA
        p[PALETTE] = s.palette
        p[SENSITIVITY] = s.sensitivity
        p[VIEW] = s.view.name
        p[REDUCE_MOTION] = s.reduceMotion
        p[REDUCE_FLASHING] = s.reduceFlashing
        p[SEATED] = s.seated
        p[SPECTRUM_PANEL] = s.spectrumPanel
        p[UI_SOUNDS] = s.uiSounds
        p[VOLUME_WIDTH] = s.volumeWidth
        p[SAFETY_SEEN] = s.safetySeen
    }

    private companion object {
        val SCHEMA = intPreferencesKey("schema")
        val PALETTE = intPreferencesKey("palette")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val VIEW = stringPreferencesKey("view")
        val REDUCE_MOTION = booleanPreferencesKey("reduceMotion")
        val REDUCE_FLASHING = booleanPreferencesKey("reduceFlashing")
        val SEATED = booleanPreferencesKey("seated")
        val SPECTRUM_PANEL = booleanPreferencesKey("spectrumPanel")
        val UI_SOUNDS = booleanPreferencesKey("uiSounds")
        val VOLUME_WIDTH = floatPreferencesKey("volumeWidth")
        val SAFETY_SEEN = booleanPreferencesKey("safetySeen")
    }
}
