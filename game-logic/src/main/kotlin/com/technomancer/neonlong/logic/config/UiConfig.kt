package com.technomancer.neonlong.logic.config

import kotlinx.serialization.Serializable

/** Control panel, badge, spectrum panel and interface sound settings. */
@Serializable
data class UiConfig(
    /** Badge pulse while nothing plays and no panel has been opened yet. */
    val badgePulseHz: Float = 1f,
    val badgePulseGain: Float = 0.2f,
    /** Spectrum panel redraw rate. */
    val spectrumHz: Float = 30f,
    /** Interface sound level, on top of the levels baked into the sound files. */
    val uiSfxGainDb: Float = -6f,
    /** How long a toast stays up, in seconds. */
    val toastS: Float = 3f,
    /** How often the Now playing line refreshes, in seconds. */
    val statusRefreshS: Float = 0.25f,
)
