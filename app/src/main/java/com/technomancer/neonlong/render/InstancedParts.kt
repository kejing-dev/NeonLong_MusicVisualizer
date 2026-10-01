package com.technomancer.neonlong.render

import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.TextureResource
import com.technomancer.neonlong.logic.dragon.ChunkChain
import com.technomancer.neonlong.logic.dragon.Fiber
import com.technomancer.neonlong.logic.dragon.PartFamily
import com.technomancer.neonlong.logic.dragon.PoseBuffer

/** Something that draws part of the dragon from the simulation's output. */
interface PartRenderer {
    /** Draw calls (entities with a mesh) this renderer uses. */
    val drawCount: Int
    /** Lit metal entities that should reflect the night environment. */
    val metalEntities: List<Entity>
    fun build(parent: Entity)
    fun apply(pose: PoseBuffer)
}

private fun isMetal(look: FamilyRenderer.Look) =
    look == FamilyRenderer.Look.METAL || look == FamilyRenderer.Look.TOWER || look == FamilyRenderer.Look.ARMOUR ||
        look == FamilyRenderer.Look.CHROME || look == FamilyRenderer.Look.GLASS

/**
 * A part family drawn with instancing in [groups] groups. If instancing can't be set up on
 * this device, falls back to one entity per part.
 */
class InstancedFamily(
    private val family: PartFamily,
    private val mesh: MeshResource,
    private val look: FamilyRenderer.Look,
    private val texture: TextureResource?,
    private val groups: Int,
) : PartRenderer {
    private val inst = InstancedRenderer(family.name, mesh, look, texture, family.capacity, groups)
    private var fallback: FamilyRenderer? = null

    override val drawCount: Int get() = if (fallback != null) family.capacity else groups
    override val metalEntities: List<Entity>
        get() = if (!isMetal(look)) emptyList() else fallback?.entities?.filterNotNull() ?: inst.entityList

    override fun build(parent: Entity) {
        inst.build(parent)
        if (!inst.ok) fallback = FamilyRenderer(family, mesh, look, texture).also { it.build(parent) }
    }

    override fun apply(pose: PoseBuffer) {
        fallback?.let { it.apply(pose); return }
        val t = pose.transforms(family)
        val a = pose.appearance(family)
        inst.begin()
        for (n in 0 until family.capacity) {
            val o = n * PoseBuffer.TRANSFORM_STRIDE
            val c = n * PoseBuffer.APPEARANCE_STRIDE
            inst.set(
                n, t[o], t[o + 1], t[o + 2], t[o + 3], t[o + 4], t[o + 5], t[o + 6], t[o + 7], t[o + 8], t[o + 9],
                a[c], a[c + 1], a[c + 2], a[c + 3] > 0.5f,
            )
        }
        inst.end()
    }
}

/** A per-entity family (for parts that need their own entity, e.g. touchable or one-off). */
class SingleFamily(
    private val renderer: FamilyRenderer,
    private val extra: ((Int, Entity) -> Unit)? = null,
) : PartRenderer {
    override val drawCount: Int get() = renderer.count
    override val metalEntities: List<Entity>
        get() = if (isMetal(renderer.look)) renderer.entities.filterNotNull() else emptyList()

    override fun build(parent: Entity) = renderer.build(parent, extra)
    override fun apply(pose: PoseBuffer) = renderer.apply(pose)
}

/**
 * Tubes drawn as chains of [chunks] stretched cylinders each, instanced in [groups] groups.
 * Only the part of each fiber between [startFrac] and [endFrac] of its length is drawn.
 */
class InstancedChains(
    name: String,
    private val fibers: Array<Fiber>,
    private val chunks: Int,
    mesh: MeshResource,
    additive: Boolean,
    private val groups: Int,
    private val startFrac: Float = 0f,
    private val endFrac: Float = 1f,
) : PartRenderer {
    private val chains = Array(fibers.size) { ChunkChain(chunks) }
    private val look = if (additive) FamilyRenderer.Look.ADDITIVE else FamilyRenderer.Look.NEON
    private val inst = InstancedRenderer(name, mesh, look, null, fibers.size * chunks, groups)
    private val fallback = ChainRenderer(name, fibers, chunks, mesh, additive, startFrac, endFrac)
    private var useFallback = false

    override val drawCount: Int get() = if (useFallback) fibers.size * chunks else groups
    override val metalEntities: List<Entity> get() = emptyList()

    override fun build(parent: Entity) {
        inst.build(parent)
        if (!inst.ok) {
            useFallback = true
            fallback.build(parent)
        }
    }

    override fun apply(pose: PoseBuffer) {
        if (useFallback) {
            fallback.apply()
            return
        }
        inst.begin()
        for (f in fibers.indices) {
            val fiber = fibers[f]
            val chain = chains[f]
            val last = fiber.points - 1
            if (fiber.visible) chain.update(fiber, (startFrac * last).toInt(), (endFrac * last).toInt())
            val t = chain.transforms
            val c = chain.rgb
            for (k in 0 until chunks) {
                val o = k * 10
                inst.set(
                    f * chunks + k, t[o], t[o + 1], t[o + 2], t[o + 3], t[o + 4], t[o + 5], t[o + 6], t[o + 7], t[o + 8], t[o + 9],
                    c[k * 3], c[k * 3 + 1], c[k * 3 + 2], fiber.visible,
                )
            }
        }
        inst.end()
    }
}
