package com.technomancer.neonlong.containers

import com.pico.spatial.ui.platform.containers.SpatialNavigator

/** Container ids of the Planar windows. */
const val CONTROLS_CONTAINER = "controls"
const val SAFETY_CONTAINER = "safety"
const val SPECTRUM_CONTAINER = "spectrum"

/** Every window is opened with this tag, so opening it again brings the same window forward. */
private const val SINGLE = "single"

fun SpatialNavigator.openControls() = openWindowContainer(CONTROLS_CONTAINER, SINGLE, null)
fun SpatialNavigator.closeControls() = closeWindowContainer(CONTROLS_CONTAINER, SINGLE)
fun SpatialNavigator.openSafety() = openWindowContainer(SAFETY_CONTAINER, SINGLE, null)
fun SpatialNavigator.closeSafety() = closeWindowContainer(SAFETY_CONTAINER, SINGLE)
fun SpatialNavigator.openSpectrum() = openWindowContainer(SPECTRUM_CONTAINER, SINGLE, null)
fun SpatialNavigator.closeSpectrum() = closeWindowContainer(SPECTRUM_CONTAINER, SINGLE)
