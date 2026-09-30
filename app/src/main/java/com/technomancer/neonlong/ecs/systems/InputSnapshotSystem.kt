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
 * First System each frame: fills the shared InputSnapshot and turns hand gestures into
 * Call / Send / Flash events. There is no audio yet, so the audio fields are cleared.
 */
class InputSnapshotSystem : System() {
    private val left = HandFrame()
    private val right = HandFrame()
    private val target = Vec3()
    private val classifier = GestureClassifier()

    override fun update(context: SceneUpdateContext) {
        val reg = ServiceRegistry
        val input = reg.input
        input.clearAudio()
        input.reducedMotion = reg.reducedMotion

        val hands = reg.handTracking ?: return
        hands.read(left, right)
        if (!left.tracked && !right.tracked) return
        if (!dragonCenterWorld(target)) return
        when (classifier.update(context.deltaTime, left, right, target)) {
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
