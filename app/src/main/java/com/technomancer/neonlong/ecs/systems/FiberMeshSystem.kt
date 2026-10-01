package com.technomancer.neonlong.ecs.systems

import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.technomancer.neonlong.app.ServiceRegistry

/** Draws the glowing tubes (core, flame tufts, tail fan, pearl flames, cables) as chains of stretched pieces. */
class FiberMeshSystem : System() {
    override fun update(context: SceneUpdateContext) {
        val reg = ServiceRegistry
        val ents = reg.dragonEntities
        if (!ents.built || reg.spikeEnabled || !reg.session.running) return
        val start = java.lang.System.nanoTime()
        ents.applyFibers(reg.pose)
        FramePerf.add(java.lang.System.nanoTime() - start)
    }
}
