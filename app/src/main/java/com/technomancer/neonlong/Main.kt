package com.technomancer.neonlong

import com.technomancer.neonlong.content.HomeVolume
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope

fun mainApp(scope: SpatialAppScope) =
    with(scope) { DefaultWindowContainer { PicoTheme { HomeVolume() } } }

