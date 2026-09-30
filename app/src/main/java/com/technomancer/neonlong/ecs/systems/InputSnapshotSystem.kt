package com.technomancer.neonlong.ecs.systems

import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.technomancer.neonlong.app.ServiceRegistry

/**
 * First System each frame: fills the shared InputSnapshot. For now there is no audio,
 * hand tracking or controller input, so it only resets the audio fields.
 */
class InputSnapshotSystem : System() {
    override fun update(context: SceneUpdateContext) {
        val input = ServiceRegistry.input
        input.clearAudio()
        input.reducedMotion = false
    }
}
