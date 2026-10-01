package com.technomancer.neonlong.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.technomancer.neonlong.app.ServiceRegistry
import kotlinx.coroutines.delay

/** Shows the current toast for a few seconds at the bottom of the panel. */
@Composable
fun Toasts() {
    val ui = ServiceRegistry.ui
    val toast by ui.toast.collectAsState()
    val shown = toast ?: return
    LaunchedEffect(shown) {
        delay((ServiceRegistry.uiConfig.toastS * 1000).toLong())
        ui.clearToast(shown)
    }
    Text(
        text = stringResource(shown.text),
        style = PicoTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(PicoTheme.colorScheme.fillTertiary, RoundedCornerShape(12.dp))
            .padding(12.dp),
    )
}
