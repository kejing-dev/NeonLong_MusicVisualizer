package com.technomancer.neonlong.app

import android.content.Context
import com.technomancer.neonlong.config.ConfigLoader
import com.technomancer.neonlong.logic.config.ComfortConfig
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.config.SceneConfig
import com.technomancer.neonlong.logic.dragon.DragonSimulation
import com.technomancer.neonlong.logic.dragon.PoseBuffer
import com.technomancer.neonlong.logic.input.InputSnapshot
import com.technomancer.neonlong.logic.state.SessionState
import com.technomancer.neonlong.input.HandTrackingAdapter
import com.technomancer.neonlong.input.InputEvents
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
    lateinit var simulation: DragonSimulation
        private set

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

    /** Reduce motion; a debug switch until the settings panel exists. */
    @Volatile
    var reducedMotion = false

    lateinit var lod: Lod
        private set

    fun init(context: Context) {
        dragonConfig = ConfigLoader.load(context, "dragon", DragonConfig.serializer(), DragonConfig())
        sceneConfig = ConfigLoader.load(context, "scene", SceneConfig.serializer(), SceneConfig())
        comfortConfig = ConfigLoader.load(context, "comfort", ComfortConfig.serializer(), ComfortConfig())
        simulation = DragonSimulation(dragonConfig, comfortConfig)
        lod = Lod(sceneConfig)
        volumeWidthMeters = sceneConfig.defaultWidth
    }
}
