package com.technomancer.neonlong.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Slider
import com.pico.spatial.ui.design.Text
import com.technomancer.neonlong.R
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.logic.state.SettingsModel

/** View: Orbit, or Ride (only in Immersive mode, so dimmed in the volume). */
@Composable
fun ViewSection() {
    FormRow(stringResource(R.string.section_view), stringResource(R.string.immersive_only)) {
        SegmentControl(modifier = Modifier.fillMaxWidth()) {
            SegmentItem(selected = true, onClick = {}, title = { Text(stringResource(R.string.view_orbit)) })
            SegmentItem(selected = false, onClick = {}, modifier = Modifier.alpha(DISABLED_ALPHA), title = { Text(stringResource(R.string.view_ride)) })
        }
    }
}

/** Sensitivity slider, 0.5–2.0 in steps of 0.05, with its value ("1.0×") under the label. */
@Composable
fun SensitivitySection() {
    val reg = ServiceRegistry
    val settings by reg.settings.state.collectAsState()
    var dragging by remember { mutableFloatStateOf(Float.NaN) }
    val shown = if (dragging.isNaN()) settings.sensitivity else SettingsModel.snapSensitivity(dragging)
    FormRow(stringResource(R.string.section_sensitivity), SettingsModel.sensitivityLabel(shown)) {
        Slider(
            value = shown,
            valueRange = SettingsModel.SENSITIVITY_MIN..SettingsModel.SENSITIVITY_MAX,
            onValueChange = { v ->
                val before = SettingsModel.snapSensitivity(if (dragging.isNaN()) settings.sensitivity else dragging)
                dragging = v
                val now = SettingsModel.snapSensitivity(v)
                if (now != before) {
                    reg.uiSfx.tick()
                    reg.settings.update { it.copy(sensitivity = now) }
                }
            },
            onValueChangeFinished = { dragging = Float.NaN },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
