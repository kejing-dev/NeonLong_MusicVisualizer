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

/** Space: Room / Immersive. Immersive mode isn't built yet, so it's dimmed, "Coming soon". */
@Composable
fun StageSection() {
    FormRow(stringResource(R.string.section_space), stringResource(R.string.coming_soon)) {
        SegmentControl(modifier = Modifier.fillMaxWidth()) {
            SegmentItem(selected = true, onClick = {}, title = { Text(stringResource(R.string.space_room)) })
            SegmentItem(selected = false, onClick = {}, modifier = Modifier.alpha(DISABLED_ALPHA), title = { Text(stringResource(R.string.space_immersive)) })
        }
    }
}
