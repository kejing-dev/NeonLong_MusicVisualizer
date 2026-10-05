package com.technomancer.neonlong.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.audio.AudioEngine
import kotlinx.coroutines.delay

/** What the panel shows about the music: read from the audio engine a few times a second. */
data class AudioStatus(
    val state: AudioEngine.State,
    val source: AudioEngine.Source,
    val fileTitle: String?,
    val loading: Boolean,
    /** Whole seconds, so the panel only redraws when the shown time changes. */
    val positionS: Int,
    val durationS: Int,
)

private fun readStatus(): AudioStatus {
    val a = ServiceRegistry.audio
    return AudioStatus(a.state, a.source, a.fileTitle, a.loading, a.positionS.toInt(), a.durationS.toInt())
}

/** The current [AudioStatus], refreshed every [com.technomancer.neonlong.logic.config.UiConfig.statusRefreshS]. */
@Composable
fun audioStatus(): AudioStatus {
    var status by remember { mutableStateOf(readStatus()) }
    LaunchedEffect(Unit) {
        val periodMs = (ServiceRegistry.uiConfig.statusRefreshS * 1000).toLong().coerceAtLeast(50)
        while (true) {
            status = readStatus()
            delay(periodMs)
        }
    }
    return status
}
