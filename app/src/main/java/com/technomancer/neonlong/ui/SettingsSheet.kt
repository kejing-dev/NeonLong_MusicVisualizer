package com.technomancer.neonlong.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.technomancer.neonlong.R
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.containers.closeSpectrum
import com.technomancer.neonlong.containers.openSafety
import com.technomancer.neonlong.containers.openSpectrum

/** Settings: Reduce motion, Reduce flashing, Seated, Spectrum panel, Interface sounds, About. */
@Composable
fun SettingsSheet(onBack: () -> Unit) {
    val reg = ServiceRegistry
    val navigator = LocalSpatialNavigator.current
    val s by reg.settings.state.collectAsState()
    Column(modifier = Modifier.fillMaxWidth()) {
        Button(onClick = onBack, modifier = Modifier.heightIn(min = minTarget())) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.settings_title), style = PicoTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
        SwitchRow(stringResource(R.string.reduce_motion), stringResource(R.string.reduce_motion_sub), s.reduceMotion) { on ->
            reg.uiSfx.select(); reg.settings.update { it.copy(reduceMotion = on) }
        }
        SwitchRow(stringResource(R.string.reduce_flashing), stringResource(R.string.reduce_flashing_sub), s.reduceFlashing) { on ->
            reg.uiSfx.select(); reg.settings.update { it.copy(reduceFlashing = on) }
        }
        SwitchRow(stringResource(R.string.seated), stringResource(R.string.seated_sub), s.seated) { on ->
            reg.uiSfx.select(); reg.settings.update { it.copy(seated = on) }
        }
        SwitchRow(stringResource(R.string.spectrum_panel), stringResource(R.string.spectrum_panel_sub), s.spectrumPanel) { on ->
            reg.uiSfx.select()
            reg.settings.update { it.copy(spectrumPanel = on) }
            if (on) navigator.openSpectrum() else navigator.closeSpectrum()
        }
        // The sound plays after the change, so turning sounds on is heard and turning them off is silent.
        SwitchRow(stringResource(R.string.ui_sounds), stringResource(R.string.ui_sounds_sub), s.uiSounds) { on ->
            reg.settings.update { it.copy(uiSounds = on) }
            reg.uiSfx.select()
        }
        SectionTitle(stringResource(R.string.about_privacy))
        Text(stringResource(R.string.about_body), style = PicoTheme.typography.bodyMedium)
        Button(
            onClick = { reg.uiSfx.select(); navigator.openSafety() },
            modifier = Modifier.padding(top = 8.dp).heightIn(min = minTarget()),
        ) { Text(stringResource(R.string.show_safety)) }
    }
}
