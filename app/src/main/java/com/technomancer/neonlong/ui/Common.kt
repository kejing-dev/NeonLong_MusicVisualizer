package com.technomancer.neonlong.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Switch
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter

/** Smallest hit target for panel controls: 32 mm. */
@Composable
fun minTarget(): Dp = LocalPhysicalLengthConverter.current.lengthToDp(0.032f, LengthUnit.Meters)

/** Gap between one option and the next: 10 mm. */
@Composable
fun optionGap(): Dp = LocalPhysicalLengthConverter.current.lengthToDp(0.010f, LengthUnit.Meters)

/** A section heading above a group of controls. */
@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = PicoTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

/** Share of a form row's width taken by its label. */
private const val LABEL_SHARE = 0.3f

/**
 * One line of the panel: the setting's name on the left (with an optional small note under
 * it) and its control on the right, at least one hit target tall.
 */
@Composable
fun FormRow(label: String, note: String? = null, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = minTarget()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(LABEL_SHARE)) {
            Text(text = label, style = PicoTheme.typography.titleSmall)
            if (note != null) Text(text = note, style = PicoTheme.typography.bodyTiny)
        }
        Box(modifier = Modifier.weight(1f - LABEL_SHARE)) { control() }
    }
}

/** A label with an optional second line and a switch on the right. */
@Composable
fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = minTarget()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = PicoTheme.typography.bodyLarge)
            if (subtitle != null) Text(text = subtitle, style = PicoTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** m:ss for a time in seconds. */
fun formatTime(seconds: Double): String {
    val s = seconds.coerceAtLeast(0.0).toInt()
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/** Opacity of controls that aren't available yet. */
const val DISABLED_ALPHA = 0.4f
