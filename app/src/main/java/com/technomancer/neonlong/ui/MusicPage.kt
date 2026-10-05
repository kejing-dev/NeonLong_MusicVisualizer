package com.technomancer.neonlong.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.util.Log
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.technomancer.neonlong.R
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.audio.sources.FileSource
import com.technomancer.neonlong.containers.openSafety

/**
 * "Your music": browse for a file with the system's file picker, or pick one from the app's
 * own Music folder (for headsets with no picker, or files copied over a cable). Picking a
 * file loads it and goes back to the main page through [onDone].
 */
@Composable
fun MusicPage(onDone: () -> Unit) {
    val reg = ServiceRegistry
    val context = LocalContext.current
    val navigator = LocalSpatialNavigator.current
    val settings by reg.settings.state.collectAsState()
    val files = remember { FileSource.listMusicFolder(context) }
    val folder = remember { FileSource.musicFolder(context)?.absolutePath ?: "" }

    // Nothing plays before the safety notice has been read.
    fun play(uri: Uri) {
        if (!settings.safetySeen) {
            reg.ui.showToast(R.string.toast_safety_first)
            navigator.openSafety()
        } else {
            reg.loadMusic(uri)
        }
        onDone()
    }
    // The system file picker, registered with the window's activity while this page is shown.
    val onPicked by rememberUpdatedState<(Uri?) -> Unit> { uri -> if (uri != null) play(uri) }
    var picker by remember { mutableStateOf<ActivityResultLauncher<Array<String>>?>(null) }
    DisposableEffect(context) {
        val launcher = context.componentActivity()?.activityResultRegistry
            ?.register(PICKER_KEY, ActivityResultContracts.OpenDocument()) { onPicked(it) }
        picker = launcher
        onDispose { launcher?.unregister() }
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(optionGap())) {
        Button(onClick = { reg.uiSfx.select(); onDone() }, modifier = Modifier.heightIn(min = minTarget())) {
            Text(stringResource(R.string.back))
        }
        Text(stringResource(R.string.music_title), style = PicoTheme.typography.titleLarge)
        Button(
            onClick = {
                reg.uiSfx.select()
                val launcher = picker
                try {
                    if (launcher == null) throw ActivityNotFoundException("no activity to open the picker from")
                    launcher.launch(arrayOf("audio/*"))
                } catch (e: ActivityNotFoundException) {
                    Log.w(LOG_TAG, "[audio] no file picker on this device: ${e.message}")
                    reg.ui.showToast(R.string.toast_no_picker)
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = minTarget()),
        ) { Text(stringResource(R.string.music_browse)) }

        SectionTitle(stringResource(R.string.music_folder_title))
        if (files.isEmpty()) {
            Text(stringResource(R.string.music_folder_empty), style = PicoTheme.typography.bodyMedium)
        } else {
            for (f in files) {
                Button(
                    onClick = { reg.uiSfx.select(); play(Uri.fromFile(f)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = minTarget()),
                ) { Text(f.nameWithoutExtension, maxLines = 1) }
            }
        }
        Text(stringResource(R.string.music_folder_hint, folder), style = PicoTheme.typography.bodySmall)
        Text(stringResource(R.string.music_privacy), style = PicoTheme.typography.bodySmall)
    }
}

private const val PICKER_KEY = "neonlong.pick.music"

/** The activity hosting this context, if it can launch other activities for a result. */
private fun Context.componentActivity(): ComponentActivity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is ComponentActivity) return c
        c = c.baseContext
    }
    return null
}
