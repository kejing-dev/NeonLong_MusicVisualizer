package com.technomancer.neonlong.app

import android.content.Context
import android.net.Uri
import com.technomancer.neonlong.R
import com.technomancer.neonlong.audio.AudioEngine
import com.technomancer.neonlong.audio.UiSfx
import com.technomancer.neonlong.config.ConfigLoader
import com.technomancer.neonlong.input.HandTrackingAdapter
import com.technomancer.neonlong.input.InputEvents
import com.technomancer.neonlong.logic.config.AudioConfig
import com.technomancer.neonlong.logic.config.ComfortConfig
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.config.FlashConfig
import com.technomancer.neonlong.logic.config.GlowConfig
import com.technomancer.neonlong.logic.config.PaletteConfig
import com.technomancer.neonlong.logic.config.SceneConfig
import com.technomancer.neonlong.logic.config.UiConfig
import com.technomancer.neonlong.logic.audio.SpectrumSnapshot
import com.technomancer.neonlong.logic.state.SettingsModel
import com.technomancer.neonlong.settings.SettingsStore
import com.technomancer.neonlong.logic.dragon.DragonSimulation
import com.technomancer.neonlong.logic.dragon.PoseBuffer
import com.technomancer.neonlong.logic.input.InputSnapshot
import com.technomancer.neonlong.logic.state.SessionState
import com.technomancer.neonlong.logic.view.Lod
import com.technomancer.neonlong.render.DragonEntities

/**
 * The app's only singleton. Holds long-lived services shared by containers and Systems,
 * which the SDK instantiates without constructor arguments. Call [init] once from the
 * Application before any container opens.
 */
object ServiceRegistry {
    val entities = EntityRegistry()
    val dragonEntities = DragonEntities()

    lateinit var dragonConfig: DragonConfig
        private set
    lateinit var sceneConfig: SceneConfig
        private set
    lateinit var comfortConfig: ComfortConfig
        private set
    lateinit var audioConfig: AudioConfig
        private set
    lateinit var flashConfig: FlashConfig
        private set
    lateinit var paletteConfig: PaletteConfig
        private set
    lateinit var simulation: DragonSimulation
        private set
    lateinit var audio: AudioEngine
        private set
    lateinit var lod: Lod
        private set
    lateinit var uiConfig: UiConfig
        private set
    lateinit var glowConfig: GlowConfig
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var uiSfx: UiSfx
        private set

    /** Which windows are open, toasts. */
    val ui = UiState()

    /** Latest analysis for the spectrum panel, copied each frame by the input System. */
    val spectrumForPanel = SpectrumSnapshot()

    val pose = PoseBuffer()
    val input = InputSnapshot()
    val session = SessionState()

    /** Current width of the dragon volume in meters, written by the UI thread. */
    @Volatile
    var volumeWidthMeters = 0f

    /** Dragon yaw set by the turntable, radians counter-clockwise from above. */
    @Volatile
    var dragonYaw = 0f

    /** Discrete input from gesture callbacks and tracking threads, drained each frame. */
    val events = InputEvents()

    /** Hand tracking while the dragon container is open; null otherwise. */
    @Volatile
    var handTracking: HandTrackingAdapter? = null

    /** Debug only: run the entity-count spike instead of the dragon. */
    @Volatile
    var spikeEnabled = false

    /** Debug only: start the demo as soon as the dragon is built. */
    @Volatile
    var autoPlay = false

    /** Debug only: a music file to load at startup, by path. */
    @Volatile
    var debugFilePath: String? = null

    /**
     * Loads one of the user's music files and plays it when it's decoded. If it can't be
     * played, a toast says so and the current music carries on.
     */
    fun loadMusic(uri: Uri) {
        audio.loadFile(
            uri,
            onReady = { events.post(InputEvents.FILE_READY) },
            onError = { ui.showToast(R.string.toast_bad_file) },
        )
    }

    /** Debug only: forces Reduce motion on, whatever the setting says. */
    @Volatile
    var reducedMotion = false

    fun init(context: Context) {
        dragonConfig = ConfigLoader.load(context, "dragon", DragonConfig.serializer(), DragonConfig())
        sceneConfig = ConfigLoader.load(context, "scene", SceneConfig.serializer(), SceneConfig())
        comfortConfig = ConfigLoader.load(context, "comfort", ComfortConfig.serializer(), ComfortConfig())
        audioConfig = ConfigLoader.load(context, "audio", AudioConfig.serializer(), AudioConfig())
        flashConfig = ConfigLoader.load(context, "flash", FlashConfig.serializer(), FlashConfig())
        paletteConfig = ConfigLoader.load(context, "palettes", PaletteConfig.serializer(), PaletteConfig())
        uiConfig = ConfigLoader.load(context, "ui", UiConfig.serializer(), UiConfig())
        glowConfig = ConfigLoader.load(context, "glow", GlowConfig.serializer(), GlowConfig())
        settings = SettingsStore(context, paletteConfig.palettes.size, sceneConfig.minWidth, sceneConfig.maxWidth)
        settings.loadBlocking()
        simulation = DragonSimulation(dragonConfig, comfortConfig, flashConfig, paletteConfig)
        // Saved settings reach the dragon before its first frame.
        SettingsModel.applyToSimulation(settings.current, simulation, input)
        audio = AudioEngine(context.applicationContext, audioConfig) { settings.current }
        uiSfx = UiSfx(uiConfig) { settings.current.uiSounds }
        lod = Lod(sceneConfig)
        val savedWidth = settings.current.volumeWidth
        volumeWidthMeters = if (savedWidth > 0f) savedWidth else sceneConfig.defaultWidth
    }
}
