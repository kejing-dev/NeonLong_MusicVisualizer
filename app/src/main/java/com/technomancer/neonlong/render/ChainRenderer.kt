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
import com.technomancer.neonlong.logic.dragon.Fiber
import kotlin.math.abs

/**
 * Draws a set of [fibers] (tubes such as flames or cables), each as a chain of [chunks]
 * stretched unlit cylinders. Only the part of each fiber between [startFrac] and [endFrac]
 * of its length is drawn.
 */
class ChainRenderer(
    private val name: String,
    private val fibers: Array<Fiber>,
    private val chunks: Int,
    private val mesh: MeshResource,
    private val additive: Boolean = false,
    private val startFrac: Float = 0f,
    private val endFrac: Float = 1f,
) {
    private val chains = Array(fibers.size) { ChunkChain(chunks) }
    private val entities = arrayOfNulls<Entity>(fibers.size * chunks)
    private val materials = arrayOfNulls<UnlitMaterial>(fibers.size * chunks)
    private val lastRgb = FloatArray(fibers.size * chunks * 3) { -1f }
    private val lastVisible = BooleanArray(fibers.size) { true }

    val count: Int get() = fibers.size * chunks

    fun build(parent: Entity) {
        for (f in fibers.indices) for (k in 0 until chunks) {
            val m = UnlitMaterial.create(if (additive) BlendingMode.ADD else BlendingMode.OPAQUE)
            val e = Entity().apply {
                setName("${name}_${f}_$k")
                components.set(ModelComponent(mesh, m))
                components.set(TransformComponent())
            }
            parent.addChild(e)
            entities[f * chunks + k] = e
            materials[f * chunks + k] = m
        }
    }

    fun apply() {
        for (f in fibers.indices) {
            val fiber = fibers[f]
            if (fiber.visible != lastVisible[f]) {
                for (k in 0 until chunks) entities[f * chunks + k]?.components?.get(ModelComponent::class.java)?.isVisible = fiber.visible
                lastVisible[f] = fiber.visible
            }
            if (!fiber.visible) continue
            val chain = chains[f]
            val last = fiber.points - 1
            chain.update(fiber, (startFrac * last).toInt(), (endFrac * last).toInt())
            val t = chain.transforms
            val c = chain.rgb
            for (k in 0 until chunks) {
                val idx = f * chunks + k
                val e = entities[idx] ?: continue
                val o = k * 10
                e.components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3(t[o], t[o + 1], t[o + 2]))
                    setQuaternion(SdkMath.quat(t[o + 3], t[o + 4], t[o + 5], t[o + 6]))
                    setScaleVector(Vector3(t[o + 7], t[o + 8], t[o + 9]))
                }
                val i = k * 3
                val l = idx * 3
                if (abs(c[i] - lastRgb[l]) + abs(c[i + 1] - lastRgb[l + 1]) + abs(c[i + 2] - lastRgb[l + 2]) > 0.01f) {
                    lastRgb[l] = c[i]; lastRgb[l + 1] = c[i + 1]; lastRgb[l + 2] = c[i + 2]
                    val s = Glow.capScale(c[i], c[i + 1], c[i + 2])
                    materials[idx]?.setBaseColor(Color4(c[i] * s, c[i + 1] * s, c[i + 2] * s, 1f))
                }
            }
        }
    }
}
