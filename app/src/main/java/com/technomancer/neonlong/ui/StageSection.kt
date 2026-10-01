package com.technomancer.neonlong.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text
import com.technomancer.neonlong.R

/** Space: Room / Immersive. Immersive mode isn't built yet, so only Room is shown selected. */
@Composable
fun StageSection() {
    SectionTitle(stringResource(R.string.section_space))
    SegmentControl(modifier = Modifier.fillMaxWidth().heightIn(min = minTarget())) {
        SegmentItem(selected = true, onClick = {}, title = { Text(stringResource(R.string.space_room)) })
        SegmentItem(selected = false, onClick = {}, modifier = Modifier.alpha(DISABLED_ALPHA), title = { Text(stringResource(R.string.space_immersive)) })
    }
    Text(stringResource(R.string.coming_soon), style = PicoTheme.typography.bodySmall)
}
