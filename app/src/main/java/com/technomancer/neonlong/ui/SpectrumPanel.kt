package com.technomancer.neonlong.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.technomancer.neonlong.R
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.logic.config.PaletteDef
import com.technomancer.neonlong.logic.ui.SpectrumPanelModel
import kotlinx.coroutines.delay

/**
 * 81 bars in the palette gradient with falling peak ticks, plus Low / Mid / High meters,
 * redrawn at the configured rate (30 Hz) from the latest analysis. Drawing allocates nothing.
 */
@Composable
fun SpectrumPanel() {
    val reg = ServiceRegistry
    val settings by reg.settings.state.collectAsState()
    val model = remember { SpectrumPanelModel() }
    val colors = remember(settings.palette) { gradient(reg.paletteConfig.palettes[settings.palette]) }
    val meterColors = remember(settings.palette) { arrayOf(colors[0], colors[40], colors[80]) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        val dt = 1f / reg.uiConfig.spectrumHz
        val periodMs = (1000f / reg.uiConfig.spectrumHz).toLong()
        while (true) {
            model.update(reg.spectrumForPanel, dt)
            frame++
            delay(periodMs)
        }
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.spectrum_title), style = PicoTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.band_low), style = PicoTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
            Text(stringResource(R.string.band_mid), style = PicoTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
            Text(stringResource(R.string.band_high), style = PicoTheme.typography.labelSmall, modifier = Modifier.padding(start = 8.dp))
        }
        Canvas(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
            @Suppress("UNUSED_EXPRESSION") frame // redraw whenever a new frame is counted
            val meterW = size.width * 0.12f
            val barsW = size.width - meterW - 8f
            val n = model.bars.size
            val step = barsW / n
            val barW = step * 0.7f
            val h = size.height
            val tick = (h * 0.02f).coerceAtLeast(1f)
            for (i in 0 until n) {
                val x = i * step
                val bh = model.bars[i] * h
                drawRect(colors[i], Offset(x, h - bh), Size(barW, bh))
                val py = h - model.peaks[i] * h
                drawRect(Color.White, Offset(x, py - tick), Size(barW, tick))
            }
            // Low / Mid / High meters on the right.
            val mx = barsW + 8f
            val mw = meterW / 3f
            for (k in 0..2) {
                val level = if (k == 0) model.low else if (k == 1) model.mid else model.high
                val lh = level * h
                drawRect(meterColors[k], Offset(mx + k * mw, h - lh), Size(mw * 0.7f, lh))
            }
        }
    }
}

/** 81 colors along the palette's three-stop gradient. */
private fun gradient(p: PaletteDef): Array<Color> {
    val s0 = hex(p.stop0); val s1 = hex(p.stop1); val s2 = hex(p.stop2)
    return Array(81) { i ->
        val u = i / 80f
        if (u <= 0.5f) lerp(s0, s1, u * 2f) else lerp(s1, s2, (u - 0.5f) * 2f)
    }
}

private fun hex(s: String): Color = Color(0xFF000000 or s.removePrefix("#").toLong(16))

private fun lerp(a: Color, b: Color, t: Float) =
    Color(a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f)
