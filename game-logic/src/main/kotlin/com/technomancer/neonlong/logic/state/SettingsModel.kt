package com.technomancer.neonlong.logic.state

import com.technomancer.neonlong.logic.audio.FeatureExtractor
import com.technomancer.neonlong.logic.dragon.DragonSimulation
import com.technomancer.neonlong.logic.input.InputSnapshot
import kotlin.math.roundToInt

/** How the user watches the dragon. Ride only exists in the immersive Stage. */
enum class ViewMode { ORBIT, RIDE }

/** The user's saved preferences. Missing values fall back to these defaults. */
data class Settings(
    val palette: Int = 0,
    val sensitivity: Float = 1f,
    val view: ViewMode = ViewMode.ORBIT,
    val reduceMotion: Boolean = false,
    val reduceFlashing: Boolean = false,
    val seated: Boolean = false,
    val spectrumPanel: Boolean = false,
    val uiSounds: Boolean = true,
    /** Last width of the dragon volume in meters, or 0 if never saved. */
    val volumeWidth: Float = 0f,
    val safetySeen: Boolean = false,
)

/** Clamps saved values and copies them onto the simulation, the flash limiter and the audio analysis. */
object SettingsModel {
    const val SCHEMA = 1
    const val SENSITIVITY_MIN = 0.5f
    const val SENSITIVITY_MAX = 2.0f
    const val SENSITIVITY_STEP = 0.05f

    /** Sensitivity clamped to 0.5–2.0 and snapped to steps of 0.05. */
    fun snapSensitivity(v: Float): Float {
        if (v.isNaN()) return 1f
        val clamped = v.coerceIn(SENSITIVITY_MIN, SENSITIVITY_MAX)
        return (clamped / SENSITIVITY_STEP).roundToInt() * SENSITIVITY_STEP
    }

    /**
     * Makes [s] valid: palette within [paletteCount], sensitivity snapped, volume width within
     * [minWidth]–[maxWidth] (0 stays 0, meaning "use the default").
     */
    fun sanitize(s: Settings, paletteCount: Int, minWidth: Float, maxWidth: Float): Settings = s.copy(
        palette = s.palette.coerceIn(0, (paletteCount - 1).coerceAtLeast(0)),
        sensitivity = snapSensitivity(s.sensitivity),
        volumeWidth = if (s.volumeWidth <= 0f || s.volumeWidth.isNaN()) 0f else s.volumeWidth.coerceIn(minWidth, maxWidth),
    )

    /** Applies the settings that drive the dragon. Ride isn't available in the volume, so it isn't applied here. */
    fun applyToSimulation(s: Settings, sim: DragonSimulation, input: InputSnapshot) {
        if (sim.palette.index != s.palette) sim.selectPalette(s.palette)
        sim.flash.reduceFlashing = s.reduceFlashing
        input.reducedMotion = s.reduceMotion
    }

    /** Applies the band sensitivity to the audio analysis. */
    fun applyToAnalysis(s: Settings, features: FeatureExtractor) {
        features.sensitivity = s.sensitivity
    }

    /** Sensitivity as shown under the slider, e.g. "1.0×" or "1.25×". */
    fun sensitivityLabel(v: Float): String {
        val hundredths = (snapSensitivity(v) * 100f).roundToInt()
        val whole = hundredths / 100
        val frac = hundredths % 100
        return when {
            frac == 0 -> "$whole.0×"
            frac % 10 == 0 -> "$whole.${frac / 10}×"
            else -> "$whole.${frac.toString().padStart(2, '0')}×"
        }
    }
}
