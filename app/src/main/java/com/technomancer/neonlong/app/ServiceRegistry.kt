package com.technomancer.neonlong.app

/**
 * The app's only singleton. Holds long-lived services shared by containers and
 * Systems, which the SDK instantiates without constructor arguments. Grows phase by phase
 * (configs, SettingsStore, AudioEngine, DragonSimulation).
 */
object ServiceRegistry {
    val entities = EntityRegistry()
}
