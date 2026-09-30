package com.technomancer.neonlong.input

import android.util.Log
import com.pico.spatial.tracking.controller.ControllerActionData
import com.pico.spatial.tracking.controller.ControllerTrackingProvider
import com.technomancer.neonlong.app.LOG_TAG

/**
 * Controller button fallbacks for the hand gestures:
 * hold B or Y for 0.5 s = Call, tap B or Y (under 0.3 s) = Send, press A or X = Flash.
 * The SDK only reports buttons in Full Space; in the volume this stays idle.
 * The listener runs on a tracking thread, so it only posts to [events].
 */
class ControllerAdapter(private val events: InputEvents) {
    private val provider = ControllerTrackingProvider()
    private var byDownNs = 0L
    private var byWasDown = false
    private var byCallSent = false
    private var axWasDown = false

    private val listener = ControllerTrackingProvider.ControllerActionListener { data -> onAction(data) }

    fun start() {
        provider.addControllerActionListener(listener)
        val result = try { provider.start().toString() } catch (e: Throwable) { "failed: ${e.message}" }
        Log.i(LOG_TAG, "[input] controller tracking start: $result, support ${provider.supportState}")
    }

    fun stop() {
        provider.removeControllerActionListener(listener)
        try { provider.stop() } catch (_: Throwable) {}
    }

    private fun onAction(data: ControllerActionData) {
        val now = System.nanoTime()
        val by = data.right.bButtonPressed || data.left.yButtonPressed
        val ax = data.right.aButtonPressed || data.left.xButtonPressed

        if (by && !byWasDown) {
            byDownNs = now
            byCallSent = false
        }
        if (by && !byCallSent && now - byDownNs >= HOLD_NS) {
            events.post(InputEvents.CALL)
            byCallSent = true
        }
        if (!by && byWasDown && !byCallSent && now - byDownNs < TAP_NS) {
            events.post(InputEvents.SEND)
        }
        byWasDown = by

        if (ax && !axWasDown) events.post(InputEvents.FLASH)
        axWasDown = ax
    }

    private companion object {
        const val HOLD_NS = 500_000_000L
        const val TAP_NS = 300_000_000L
    }
}
