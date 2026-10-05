package com.technomancer.neonlong.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.SegmentControl
import com.pico.spatial.ui.design.SegmentItem
import com.pico.spatial.ui.design.Text
import com.technomancer.neonlong.R
import com.technomancer.neonlong.app.ServiceRegistry

/** Source picker. Only the demo track exists for now; the others are dimmed, "Coming soon". */
@Composable
fun SourceSection() {
    FormRow(stringResource(R.string.section_source), stringResource(R.string.coming_soon)) {
        SegmentControl(modifier = Modifier.fillMaxWidth()) {
            SegmentItem(selected = true, onClick = {}, title = { Text(stringResource(R.string.source_demo_short)) })
            for (name in listOf(R.string.source_file, R.string.source_capture, R.string.source_mic)) {
                SegmentItem(selected = false, onClick = {}, modifier = Modifier.alpha(DISABLED_ALPHA), title = { Text(stringResource(name)) })
            }
        }
    }
}
