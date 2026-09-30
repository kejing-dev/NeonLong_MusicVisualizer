package com.technomancer.neonlong

import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.technomancer.neonlong.containers.DragonVolume

/**
 * Declares every container. Phase 0: only the template's Volumetric container,
 * which becomes `dragon`. `controls`, `spectrum`, `safety` and Stage `night` arrive in Phases 5–7.
 */
fun mainApp(scope: SpatialAppScope) =
    with(scope) { DefaultWindowContainer { PicoTheme { DragonVolume() } } }
