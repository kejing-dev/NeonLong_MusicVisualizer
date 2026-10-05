package com.technomancer.neonlong.ecs.systems

import android.util.Log
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.input.InputEvents
import com.technomancer.neonlong.logic.input.GestureClassifier
import com.technomancer.neonlong.logic.input.HandFrame
import com.technomancer.neonlong.logic.math.Vec3

/**
 * First System each frame: fills the shared InputSnapshot from the newest audio analysis and turns hand gestures into
 * Call / Send / Flash events.
 */
class InputSnapshotSystem : System() {
    private val left = HandFrame()
    private val right = HandFrame()
    private val target = Vec3()
    private val classifier = GestureClassifier()

    private var lastBeatCount = 0L

    override fun update(context: SceneUpdateContext) {
        val start = java.lang.System.nanoTime()
        readAudio()
        readHands(context.deltaTime)
        FramePerf.add(java.lang.System.nanoTime() - start)
    }

    /** Copies the newest analysis result; a beat counts once, on the first frame that sees it. */
    private fun readAudio() {
        val reg = ServiceRegistry
        val input = reg.input
        val snap = reg.audio.snapshots.read()
        java.lang.System.arraycopy(snap.bands, 0, input.bands, 0, input.bands.size)
        input.bass = snap.bass
        input.mid = snap.mid
        input.high = snap.high
        input.vol = snap.vol
        input.beatPulse = snap.beatPulse
        input.hasAudio = snap.hasAudio
        input.playbackPositionS = snap.positionS
        input.beatStrength = if (snap.beatCount != lastBeatCount) snap.lastBeatStrength else 0f
        lastBeatCount = snap.beatCount
        input.reducedMotion = reg.reducedMotion || reg.settings.current.reduceMotion
        reg.spectrumForPanel.copyFrom(snap)
        input.dragonYaw = reg.dragonYaw
        val width = reg.volumeWidthMeters
        input.showSmallParts = reg.lod.showSmallParts(width)
        input.whiskerStride = reg.lod.whiskerStride(width)
    }

    private fun readHands(dt: Float) {
        val reg = ServiceRegistry
        val hands = reg.handTracking ?: return
        hands.read(left, right)
        if (!left.tracked && !right.tracked) return
        if (!dragonCenterWorld(target)) return
        when (classifier.update(dt, left, right, target)) {
            GestureClassifier.Gesture.CALL -> reg.events.post(InputEvents.CALL)
            GestureClassifier.Gesture.SEND -> reg.events.post(InputEvents.SEND)
            GestureClassifier.Gesture.CLAP -> reg.events.post(InputEvents.FLASH)
            GestureClassifier.Gesture.NONE -> Unit
        }
    }

    /** World position of the model box's center, where the palm must face for a Call. */
    private fun dragonCenterWorld(out: Vec3): Boolean {
        val reg = ServiceRegistry
        val root = reg.dragonEntities.root ?: return false
        return try {
            val p = root.convertPositionTo(
                Vector3(reg.dragonConfig.boundsCenterX, reg.dragonConfig.boundsCenterY, reg.dragonConfig.boundsCenterZ),
                null,
            )
            out.set(p.x, p.y, p.z)
            true
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "[input] can't place the gesture target: ${e.message}")
            false
        }
    }
}
