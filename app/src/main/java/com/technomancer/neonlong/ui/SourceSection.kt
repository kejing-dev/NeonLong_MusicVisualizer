package com.technomancer.neonlong.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text
import com.technomancer.neonlong.R
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.audio.AudioEngine
import com.technomancer.neonlong.input.InputEvents

/**
 * Source picker: the demo track or the user's own files. [onPickFiles] opens the music page.
 * Other apps and the microphone are dimmed until they exist.
 */
@Composable
fun SourceSection(status: AudioStatus, onPickFiles: () -> Unit) {
    val reg = ServiceRegistry
    val file = status.source == AudioEngine.Source.FILE
    FormRow(stringResource(R.string.section_source)) {
        SegmentControl(modifier = Modifier.fillMaxWidth()) {
            SegmentItem(
                selected = !file,
                onClick = {
                    if (file) {
                        reg.uiSfx.select()
                        reg.events.post(InputEvents.USE_DEMO)
                    }
                },
                title = { Text(stringResource(R.string.source_demo_short)) },
            )
            // Always opens the music page, so another file can be picked while one plays.
            SegmentItem(
                selected = file,
                onClick = { reg.uiSfx.select(); onPickFiles() },
                title = { Text(stringResource(R.string.source_file)) },
            )
            for (name in listOf(R.string.source_capture, R.string.source_mic)) {
                SegmentItem(selected = false, onClick = {}, modifier = Modifier.alpha(DISABLED_ALPHA), title = { Text(stringResource(name)) })
            }
        }
    }
}
