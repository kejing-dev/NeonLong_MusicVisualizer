package com.technomancer.neonlong.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import android.util.Log
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.ui.ControlPanel

/** Content of the `controls` Planar window, opened and closed by the 龍 badge. */
@Composable
fun ControlsWindow() {
    val ui = ServiceRegistry.ui
    DisposableEffect(Unit) {
        ui.controlsOpen = true
        ui.panelOpenedOnce = true
        Log.i(LOG_TAG, "[ui] $CONTROLS_CONTAINER opened")
        onDispose {
            ui.controlsOpen = false
            Log.i(LOG_TAG, "[ui] $CONTROLS_CONTAINER closed")
        }
    }
    ControlPanel()
}
