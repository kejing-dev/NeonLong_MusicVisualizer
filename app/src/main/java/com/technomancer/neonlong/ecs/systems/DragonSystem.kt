package com.technomancer.neonlong.ecs.systems

import android.util.Log
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.SdkMath
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.input.InputEvents
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.dragon.PartFamily
import com.technomancer.neonlong.logic.dragon.PoseBuffer

/**
 * Applies queued input (touches, Call / Send / Flash), steps the dragon simulation and
 * copies the dorsal segment poses and glow onto the gray-box entities. Skips the step while
 * the container is hidden or paused, with no catch-up. Logs its own cost every few seconds.
 */
class DragonSystem : System() {
    private val samplesNs = LongArray(PERF_WINDOW)
    private val sorted = LongArray(PERF_WINDOW)
    private var sampleCount = 0
    private var lastLogNs = 0L
    private var wasRunning = false
    private var smallPartsShown: Boolean? = null

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

        val sim = reg.simulation
        reg.events.drain { kind, arg ->
            when (kind) {
                InputEvents.SEGMENT_TOUCH -> if (sim.touchSegment(arg)) Log.i(LOG_TAG, "[input] ripple from segment $arg")
                InputEvents.CALL -> { sim.call(); Log.i(LOG_TAG, "[input] call") }
                InputEvents.SEND -> { sim.send(); Log.i(LOG_TAG, "[input] send") }
                InputEvents.FLASH -> { sim.flash(); Log.i(LOG_TAG, "[input] flash") }
                InputEvents.BADGE -> Log.i(LOG_TAG, "[input] controls requested (badge)")
            }
        }

        val running = reg.session.running
        if (running != wasRunning) {
            Log.i(LOG_TAG, "[boot] simulation ${if (running) "running" else "paused"} (${reg.session.visibility})")
            wasRunning = running
        }
        if (!running) return

        sim.tick(context.deltaTime, reg.input, reg.pose)
        val t = reg.pose.transforms(PartFamily.SEG_DORSAL)
        val a = reg.pose.appearance(PartFamily.SEG_DORSAL)
        for (i in 0 until DragonConfig.SEGMENTS) {
            val transform = ents.segments[i]?.components?.get(TransformComponent::class.java) ?: continue
            val o = i * PoseBuffer.TRANSFORM_STRIDE
            // The SDK's math types are immutable, so each write allocates small objects.
            transform.setPosition(Vector3(t[o], t[o + 1], t[o + 2]))
            transform.setQuaternion(SdkMath.quat(t[o + 3], t[o + 4], t[o + 5], t[o + 6]))
            transform.setScaleVector(Vector3(t[o + 7], t[o + 8], t[o + 9]))
            val c = i * PoseBuffer.APPEARANCE_STRIDE
            ents.segmentMaterials[i]?.setEmissiveColor(Color4(a[c], a[c + 1], a[c + 2], 1f))
        }
        recordPerf(java.lang.System.nanoTime() - start)
    }

    /** Hides small parts in small volumes. Only logged until those parts exist. */
    private fun applyLod(width: Float) {
        val show = ServiceRegistry.lod.showSmallParts(width)
        if (show != smallPartsShown) {
            smallPartsShown = show
            Log.i(LOG_TAG, "[boot] LOD: small parts ${if (show) "shown" else "hidden"} at $width m (whisker stride ${ServiceRegistry.lod.whiskerStride(width)})")
        }
    }

    private fun recordPerf(ns: Long) {
        samplesNs[sampleCount % PERF_WINDOW] = ns
        sampleCount++
        val now = java.lang.System.nanoTime()
        if (now - lastLogNs < PERF_LOG_INTERVAL_NS) return
        lastLogNs = now
        val n = if (sampleCount < PERF_WINDOW) sampleCount else PERF_WINDOW
        if (n == 0) return
        var sum = 0L
        for (i in 0 until n) {
            sorted[i] = samplesNs[i]
            sum += samplesNs[i]
        }
        java.util.Arrays.sort(sorted, 0, n)
        val p95 = sorted[((n - 1) * 95) / 100]
        Log.i(LOG_TAG, "[perf] DragonSystem avg=${sum / n / 1000}us p95=${p95 / 1000}us over $n frames")
    }

    private companion object {
        const val PERF_WINDOW = 360
        const val PERF_LOG_INTERVAL_NS = 5_000_000_000L
    }
}
