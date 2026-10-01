package com.technomancer.neonlong.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/**
 * Photosensitivity warning, shown on first launch before any audio can play and from About.
 * "Start" marks it as read and closes the window.
 */
@Composable
fun SafetyNotice() {
    val reg = ServiceRegistry
    val navigator = LocalSpatialNavigator.current
    val s by reg.settings.state.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("龍", style = PicoTheme.typography.displaySmall)
        Text(stringResource(R.string.safety_title), style = PicoTheme.typography.titleLarge)
        Text(stringResource(R.string.safety_body), style = PicoTheme.typography.bodyLargeMultiline)
        Text(stringResource(R.string.safety_limit), style = PicoTheme.typography.bodyMedium)
        SwitchRow(stringResource(R.string.reduce_flashing), stringResource(R.string.reduce_flashing_sub), s.reduceFlashing) { on ->
            reg.uiSfx.select(); reg.settings.update { it.copy(reduceFlashing = on) }
        }
        Button(
            onClick = {
                reg.uiSfx.select()
                reg.settings.update { it.copy(safetySeen = true) }
                navigator.closeWindowContainer()
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = minTarget()),
        ) { Text(stringResource(R.string.start)) }
    }
}
