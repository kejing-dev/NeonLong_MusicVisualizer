package com.technomancer.neonlong.platform

import android.app.Application
import com.pico.spatial.ui.foundation.dsl.launch
import com.technomancer.neonlong.app.BootInfo
import com.technomancer.neonlong.mainApp

class SpatialApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BootInfo.log(this)
        launch(::mainApp)
    }
}
