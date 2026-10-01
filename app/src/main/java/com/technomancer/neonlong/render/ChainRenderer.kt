package com.technomancer.neonlong.render

import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.SdkMath
import com.technomancer.neonlong.logic.dragon.ChunkChain
import kotlin.math.abs

/** Draws a [ChunkChain] as stretched unlit cylinders, one entity per piece. */
class ChainRenderer(private val name: String, private val chain: ChunkChain, private val mesh: MeshResource) {
    val entities = arrayOfNulls<Entity>(chain.chunks)
    private val materials = arrayOfNulls<UnlitMaterial>(chain.chunks)
    private val lastRgb = FloatArray(chain.chunks * 3) { -1f }

    fun build(parent: Entity) {
        for (k in 0 until chain.chunks) {
            val m = UnlitMaterial.create(BlendingMode.OPAQUE)
            val e = Entity().apply {
                setName("${name}_$k")
                components.set(ModelComponent(mesh, m))
                components.set(TransformComponent())
            }
            parent.addChild(e)
            entities[k] = e
            materials[k] = m
        }
    }

    fun apply() {
        val t = chain.transforms
        val c = chain.rgb
        for (k in 0 until chain.chunks) {
            val e = entities[k] ?: continue
            val o = k * 10
            e.components[TransformComponent::class.java]?.apply {
                setPosition(Vector3(t[o], t[o + 1], t[o + 2]))
                setQuaternion(SdkMath.quat(t[o + 3], t[o + 4], t[o + 5], t[o + 6]))
                setScaleVector(Vector3(t[o + 7], t[o + 8], t[o + 9]))
            }
            val i = k * 3
            if (abs(c[i] - lastRgb[i]) + abs(c[i + 1] - lastRgb[i + 1]) + abs(c[i + 2] - lastRgb[i + 2]) > 0.01f) {
                lastRgb[i] = c[i]; lastRgb[i + 1] = c[i + 1]; lastRgb[i + 2] = c[i + 2]
                val s = Glow.capScale(c[i], c[i + 1], c[i + 2])
                materials[k]?.setBaseColor(Color4(c[i] * s, c[i + 1] * s, c[i + 2] * s, 1f))
            }
        }
    }
}
