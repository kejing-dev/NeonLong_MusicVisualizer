package com.technomancer.neonlong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Button
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.ScrollIndicator
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.foundation.layout.zOffset
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.pico.spatial.ui.platform.containers.LocalSpatialNavigator
import com.technomancer.neonlong.R
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.audio.AudioEngine
import com.technomancer.neonlong.containers.openSafety
import com.technomancer.neonlong.input.InputEvents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The control panel: a header with a Hide button, a Now playing card, transport, then one
 * line each for source, palette, view, sensitivity and space (name left, control right), and a
 * link to Settings, top to bottom. Settings open in place of the main page. The content
 * scrolls under the header; a "More" button and a scroll bar show when there's more below.
 */
@Composable
fun ControlPanel() {
    var page by remember { mutableStateOf(Page.MAIN) }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    // Each page starts at its top.
    LaunchedEffect(page) { scroll.scrollTo(0) }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        // The header stays in place; everything under it scrolls.
        PanelHeader()
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Every option is separated from the next by the same gap.
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(optionGap()),
            ) {
                when (page) {
                    Page.MAIN -> {
                        NowPlayingCard()
                        TransportRow()
                        SourceSection()
                        PaletteSection()
                        ViewSection()
                        SensitivitySection()
                        StageSection()
                        Button(
                            onClick = { ServiceRegistry.uiSfx.select(); page = Page.SETTINGS },
                            modifier = Modifier.fillMaxWidth().heightIn(min = minTarget()),
                        ) { Text(stringResource(R.string.open_settings)) }
                    }
                    Page.SETTINGS -> SettingsSheet(onBack = { ServiceRegistry.uiSfx.select(); page = Page.MAIN })
                }
                Toasts()
            }
            // A bar on the right shows the position while scrolling.
            ScrollIndicator(state = scroll, orientation = Orientation.Vertical)
            // While there's more below, a button at the bottom says so and scrolls a page down.
            if (scroll.canScrollForward) {
                Button(
                    onClick = {
                        ServiceRegistry.uiSfx.select()
                        scope.launch { scroll.animateScrollTo((scroll.value + scroll.viewportSize * 3 / 4).coerceAtMost(scroll.maxValue)) }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                ) { Text(stringResource(R.string.more_below)) }
            }
        }
    }
}

private enum class Page { MAIN, SETTINGS }

/** The status dot in the Now playing card, and the space between it and the title. */
private val DOT_SIZE = 10.dp
private val DOT_GAP = 8.dp

/** The app's name and a button that hides the panel. The 龍 badge brings it back. */
@Composable
private fun PanelHeader() {
    val navigator = LocalSpatialNavigator.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = optionGap()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.controls_title), style = PicoTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Button(
            onClick = { ServiceRegistry.uiSfx.select(); navigator.closeWindowContainer() },
            modifier = Modifier.heightIn(min = minTarget()),
        ) { Text(stringResource(R.string.hide_panel)) }
    }
}

/** What's playing, how far in, and a status dot. Sits 6 mm in front of the panel. */
@Composable
private fun NowPlayingCard() {
    val reg = ServiceRegistry
    val lengths = LocalPhysicalLengthConverter.current
    val lift = lengths.lengthToDp(0.006f, LengthUnit.Meters)
    var state by remember { mutableStateOf(reg.audio.state) }
    var position by remember { mutableStateOf(0.0) }
    LaunchedEffect(Unit) {
        val periodMs = (reg.uiConfig.statusRefreshS * 1000).toLong().coerceAtLeast(50)
        while (true) {
            state = reg.audio.state
            position = reg.audio.positionS
            delay(periodMs)
        }
    }
    val idle = state == AudioEngine.State.IDLE
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zOffset { lift.toPx() }
            .background(PicoTheme.colorScheme.fillTertiary, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DOT_GAP)) {
            val dot = when (state) {
                AudioEngine.State.PLAYING -> Color(0xFF3DDC84)
                AudioEngine.State.PAUSED -> Color(0xFFFFB627)
                // Dark enough to show against the card when nothing is playing.
                AudioEngine.State.IDLE -> Color(0xFF4A4650)
            }
            Box(Modifier.size(DOT_SIZE).background(dot, CircleShape))
            Text(stringResource(R.string.source_demo), style = PicoTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            // The time sits on the title's line, so the card stays two lines tall.
            if (!idle) {
                Text(
                    text = stringResource(R.string.status_time, formatTime(position), formatTime(reg.audio.durationS)),
                    style = PicoTheme.typography.bodySmall,
                )
            }
        }
        Text(
            text = if (idle) stringResource(R.string.status_idle) else stringResource(R.string.status_demo),
            style = PicoTheme.typography.bodySmall,
            // Lined up under the title, past the status dot.
            modifier = Modifier.padding(start = DOT_SIZE + DOT_GAP),
        )
    }
}

/** Play / Pause and Restart. Before the safety notice has been read, Play opens it instead. */
@Composable
private fun TransportRow() {
    val reg = ServiceRegistry
    val navigator = LocalSpatialNavigator.current
    val settings by reg.settings.state.collectAsState()
    var playing by remember { mutableStateOf(reg.audio.state == AudioEngine.State.PLAYING) }
    LaunchedEffect(Unit) {
        while (true) {
            playing = reg.audio.state == AudioEngine.State.PLAYING
            delay(100)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = {
                reg.uiSfx.select()
                if (!settings.safetySeen) {
                    reg.ui.showToast(R.string.toast_safety_first)
                    navigator.openSafety()
                } else {
                    reg.events.post(InputEvents.PLAY_PAUSE)
                    playing = !playing
                }
            },
            modifier = Modifier.weight(1f).heightIn(min = minTarget()),
        ) { Text(stringResource(if (playing) R.string.pause else R.string.play)) }
        Button(
            onClick = { reg.uiSfx.select(); reg.events.post(InputEvents.RESTART) },
            enabled = settings.safetySeen && reg.audio.state != AudioEngine.State.IDLE,
            modifier = Modifier.weight(1f).heightIn(min = minTarget()),
        ) { Text(stringResource(R.string.restart)) }
    }
}
