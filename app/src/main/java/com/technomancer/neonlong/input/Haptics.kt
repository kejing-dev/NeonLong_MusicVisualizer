package com.technomancer.neonlong.input

import android.util.Log
import com.pico.spatial.ui.platform.HandController
import com.pico.spatial.ui.platform.HandControllerHapticType
import com.pico.spatial.ui.platform.SpatialHandControllerHaptic
import com.technomancer.neonlong.app.LOG_TAG

/**
 * Controller vibration presets. Amplitude 0..1 maps to the SDK's 0–255 level. The tap and
 * drag callbacks don't say which controller acted, so pulses go to the right controller.
 */
class Haptics(private val haptic: SpatialHandControllerHaptic) {
    fun badge() = pulse(10, 0.3f)
    fun panelControl() = pulse(10, 0.2f)
    fun segment() = pulse(15, 0.5f)
    fun turntableTick() = pulse(5, 0.3f)
    fun snapTurn() = pulse(5, 0.2f)

    fun flash() {
        pulse(20, 0.6f, HandController.Left)
        pulse(20, 0.6f, HandController.Right)
    }

    private fun pulse(ms: Int, amplitude: Float, hand: HandController = HandController.Right) {
        try {
            haptic.feedback(HandControllerHapticType((amplitude * 255f).toInt(), FREQUENCY_HZ, ms), hand)
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "[input] haptic failed: ${e.message}")
        }
    }

    private companion object {
        const val FREQUENCY_HZ = 160
    }
}
