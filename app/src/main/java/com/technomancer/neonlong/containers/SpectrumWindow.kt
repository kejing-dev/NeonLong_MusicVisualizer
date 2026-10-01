package com.technomancer.neonlong.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import android.util.Log
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.ui.SpectrumPanel

/** Content of the optional `spectrum` Planar window. */
@Composable
fun SpectrumWindow() {
    DisposableEffect(Unit) {
        Log.i(LOG_TAG, "[ui] $SPECTRUM_CONTAINER opened")
        onDispose { Log.i(LOG_TAG, "[ui] $SPECTRUM_CONTAINER closed") }
    }
    SpectrumPanel()
}
