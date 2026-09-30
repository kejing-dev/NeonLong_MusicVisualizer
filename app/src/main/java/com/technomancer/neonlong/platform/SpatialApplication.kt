package com.technomancer.neonlong.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.technomancer.neonlong.BuildConfig
import com.technomancer.neonlong.app.BootInfo
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.debug.DebugFlags
import com.technomancer.neonlong.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BootInfo.log(this)
        ServiceRegistry.init(this)
        if (BuildConfig.DEBUG) {
            ServiceRegistry.spikeEnabled = DebugFlags.systemFlag("debug.neonlong.spike")
            ServiceRegistry.reducedMotion = DebugFlags.systemFlag("debug.neonlong.reducedmotion")
        }
        launch(::mainApp)
    }
}
