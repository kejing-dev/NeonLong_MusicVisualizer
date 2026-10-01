package com.technomancer.neonlong.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text
import com.technomancer.neonlong.R
import com.technomancer.neonlong.app.ServiceRegistry

/** Palette: Kowloon / Imperial / Jade. */
@Composable
fun PaletteSection() {
    val reg = ServiceRegistry
    val settings by reg.settings.state.collectAsState()
    val names = listOf(R.string.palette_kowloon, R.string.palette_imperial, R.string.palette_jade)
    SectionTitle(stringResource(R.string.section_palette))
    SegmentControl(modifier = Modifier.fillMaxWidth().heightIn(min = minTarget())) {
        names.forEachIndexed { i, name ->
            SegmentItem(
                selected = settings.palette == i,
                onClick = {
                    if (settings.palette != i) {
                        reg.uiSfx.select()
                        reg.settings.update { it.copy(palette = i) }
                    }
                },
                title = { Text(stringResource(name)) },
            )
        }
    }
}
