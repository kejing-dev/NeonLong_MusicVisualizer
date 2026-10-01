package com.technomancer.neonlong.ecs.systems

import android.util.Log
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.audio.AudioEngine
import com.technomancer.neonlong.input.InputEvents
import com.technomancer.neonlong.logic.state.Settings
import com.technomancer.neonlong.logic.state.SettingsModel

/**
 * Applies queued input (touches, transport, Call / Send / Flash) and changed settings, steps the dragon simulation
 * and copies the rigid body parts onto their entities. Skips the step while the container
 * is hidden or paused, with no catch-up; hiding also pauses the demo track.
 */
class DragonSystem : System() {
    private var wasRunning = false
    private var smallPartsShown: Boolean? = null
    private var appliedSettings: Settings? = null

    override fun update(context: SceneUpdateContext) {
        val reg = ServiceRegistry
        val ents = reg.dragonEntities
        if (!ents.built || reg.spikeEnabled) return
        val start = java.lang.System.nanoTime()

        val width = reg.volumeWidthMeters.coerceIn(reg.sceneConfig.minWidth, reg.sceneConfig.maxWidth)
        val yaw = reg.dragonYaw
        if (width != ents.appliedWidth || yaw != ents.appliedYaw) {
            val widthChanged = width != ents.appliedWidth
            val k = ents.place(width, yaw, reg.sceneConfig, reg.dragonConfig)
            if (widthChanged) {
                Log.i(LOG_TAG, "[boot] volume width $width m, $k m per model unit")
                applyLod(width)
            }
        }

        val settings = reg.settings.current
        if (settings !== appliedSettings) {
            SettingsModel.applyToSimulation(settings, reg.simulation, reg.input)
            appliedSettings = settings
        }

        if (reg.autoPlay && settings.safetySeen) {
            reg.autoPlay = false
            reg.audio.play(ents.root)
        }

        val sim = reg.simulation
        reg.events.drain { kind, arg ->
            when (kind) {
                InputEvents.SEGMENT_TOUCH -> if (sim.touchSegment(arg)) Log.i(LOG_TAG, "[input] ripple from segment $arg")
                InputEvents.CALL -> { sim.call(); Log.i(LOG_TAG, "[input] call") }
                InputEvents.SEND -> { sim.send(); Log.i(LOG_TAG, "[input] send") }
                InputEvents.FLASH -> { sim.flash(); Log.i(LOG_TAG, "[input] flash") }
                InputEvents.PLAY_PAUSE -> if (reg.settings.current.safetySeen) reg.audio.togglePlay(ents.root)
                InputEvents.RESTART -> reg.audio.restart()
            }
        }

        val running = reg.session.running
        if (running != wasRunning) {
            Log.i(LOG_TAG, "[boot] simulation ${if (running) "running" else "paused"} (${reg.session.visibility})")
            if (!running) reg.audio.onHidden()
            wasRunning = running
        }
        if (!running) return

        sim.tick(context.deltaTime, reg.input, reg.pose)
        ents.applyParts(reg.pose)
        // The badge pulses until the panel has been opened once, while nothing plays.
        val pulsing = !reg.ui.panelOpenedOnce && reg.audio.state != AudioEngine.State.PLAYING
        ents.updateBadge(sim.time, pulsing, reg.uiConfig)
        val ns = java.lang.System.nanoTime() - start
        FramePerf.add(ns)
        FramePerf.currentDragonNs = ns
    }

    /** Hides small parts in small volumes. Only logged until those parts exist. */
    private fun applyLod(width: Float) {
        val show = ServiceRegistry.lod.showSmallParts(width)
        if (show != smallPartsShown) {
            smallPartsShown = show
            Log.i(LOG_TAG, "[boot] LOD: small parts ${if (show) "shown" else "hidden"} at $width m (whisker stride ${ServiceRegistry.lod.whiskerStride(width)})")
        }
    }
}
