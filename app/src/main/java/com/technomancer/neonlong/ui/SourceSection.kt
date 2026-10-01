package com.technomancer.neonlong.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text
import com.technomancer.neonlong.R

/** Source picker. Only the demo track exists for now; the others show "Coming soon". */
@Composable
fun SourceSection() {
    SectionTitle(stringResource(R.string.section_source))
    SegmentControl(modifier = Modifier.fillMaxWidth().heightIn(min = minTarget())) {
        SegmentItem(selected = true, onClick = {}, title = { Text(stringResource(R.string.source_demo)) })
        SegmentItem(selected = false, onClick = {}, modifier = Modifier.alpha(DISABLED_ALPHA), title = { Text(stringResource(R.string.source_file)) })
        SegmentItem(selected = false, onClick = {}, modifier = Modifier.alpha(DISABLED_ALPHA), title = { Text(stringResource(R.string.source_capture)) })
        SegmentItem(selected = false, onClick = {}, modifier = Modifier.alpha(DISABLED_ALPHA), title = { Text(stringResource(R.string.source_mic)) })
    }
    Text(stringResource(R.string.coming_soon), style = PicoTheme.typography.bodySmall)
}
