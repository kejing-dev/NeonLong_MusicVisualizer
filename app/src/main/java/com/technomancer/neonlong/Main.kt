package com.technomancer.neonlong

import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.platform.LengthUnit
import com.technomancer.neonlong.containers.CONTROLS_CONTAINER
import com.technomancer.neonlong.containers.ControlsWindow
import com.technomancer.neonlong.containers.DragonVolume
import com.technomancer.neonlong.containers.SAFETY_CONTAINER
import com.technomancer.neonlong.containers.SPECTRUM_CONTAINER
import com.technomancer.neonlong.containers.SafetyWindow
import com.technomancer.neonlong.containers.SpectrumWindow

/**
 * Declares every container: the `dragon` volume (the launch container, configured in the
 * manifest) and the Planar `controls`, `safety` and `spectrum` windows. The Stage `night`
 * arrives with Immersive mode.
 */
fun mainApp(scope: SpatialAppScope) =
    with(scope) {
        DefaultWindowContainer { PicoTheme { DragonVolume() } }
        WindowContainer(
            CONTROLS_CONTAINER,
            form = Form.Planar,
            defaultSize = WindowContainerSize(0.40f, 0.48f, 0f, LengthUnit.Meters),
        ) { PicoTheme { ControlsWindow() } }
        WindowContainer(
            SAFETY_CONTAINER,
            form = Form.Planar,
            defaultSize = WindowContainerSize(0.50f, 0.40f, 0f, LengthUnit.Meters),
        ) { PicoTheme { SafetyWindow() } }
        WindowContainer(
            SPECTRUM_CONTAINER,
            form = Form.Planar,
            defaultSize = WindowContainerSize(0.30f, 0.08f, 0f, LengthUnit.Meters),
        ) { PicoTheme { SpectrumWindow() } }
    }
