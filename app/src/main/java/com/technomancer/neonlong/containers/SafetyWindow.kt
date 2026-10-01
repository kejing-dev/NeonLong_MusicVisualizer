package com.technomancer.neonlong.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import android.util.Log
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.ui.SafetyNotice

/** Content of the `safety` Planar window: the photosensitivity notice. */
@Composable
fun SafetyWindow() {
    val ui = ServiceRegistry.ui
    DisposableEffect(Unit) {
        ui.safetyOpen = true
        Log.i(LOG_TAG, "[ui] $SAFETY_CONTAINER opened")
        onDispose {
            ui.safetyOpen = false
            Log.i(LOG_TAG, "[ui] $SAFETY_CONTAINER closed")
        }
    }
    SafetyNotice()
}
