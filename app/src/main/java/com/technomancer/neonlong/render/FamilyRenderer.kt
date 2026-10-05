package com.technomancer.neonlong.render

import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.SdkMath
import com.technomancer.neonlong.logic.dragon.PartFamily
import com.technomancer.neonlong.logic.dragon.PoseBuffer
import kotlin.math.abs

/**
 * Draws one part family as one entity per instance (every instance is real 3D geometry).
 * Each instance has its own material so it can glow on its own; colors are only pushed to
 * the SDK when they change noticeably.
 */
class FamilyRenderer(
    val family: PartFamily,
    private val mesh: MeshResource,
    val look: Look,
    private val emissiveTexture: TextureResource? = null,
    /** A different model per instance, if given (e.g. the head's color zones). */
    private val meshes: List<MeshResource>? = null,
) {
    /**
     * METAL / TOWER / ARMOUR: lit metal with a glowing emissive map. CHROME: plain polished
     * metal. GLASS: dark see-through metal. NEON: unlit color. ADDITIVE: unlit, adds light
     * (glows). GLYPH: unlit textured plate that adds light. HALO: a soft textured glow that
     * adds light.
     */
    enum class Look { METAL, TOWER, ARMOUR, CHROME, GLASS, NEON, ADDITIVE, GLYPH, HALO }

    val count = family.capacity
    val entities = arrayOfNulls<Entity>(count)
    private val pbr = arrayOfNulls<PhysicallyBasedMaterial>(count)
    private val unlit = arrayOfNulls<UnlitMaterial>(count)
    private val lastRgb = FloatArray(count * 3) { -1f }
    private val lastVisible = BooleanArray(count) { true }

    /** Creates the entities under [parent]; [extra] adds components (e.g. touch) per instance. */
    fun build(parent: Entity, extra: ((index: Int, entity: Entity) -> Unit)? = null) {
        for (n in 0 until count) {
            val material = Looks.material(look, emissiveTexture)
            pbr[n] = material as? PhysicallyBasedMaterial
            unlit[n] = material as? UnlitMaterial
            val e = Entity().apply {
                setName("${family.name}_$n")
                components.set(ModelComponent(meshes?.get(n) ?: mesh, material))
                components.set(TransformComponent())
            }
            extra?.invoke(n, e)
            parent.addChild(e)
            entities[n] = e
        }
    }

    /** Copies this family's poses and glow from [pose] onto the entities. */
    fun apply(pose: PoseBuffer) {
        val t = pose.transforms(family)
        val a = pose.appearance(family)
        for (n in 0 until count) {
            val e = entities[n] ?: continue
            val c = n * PoseBuffer.APPEARANCE_STRIDE
            val visible = a[c + 3] > 0.5f
            if (visible != lastVisible[n]) {
                e.components[ModelComponent::class.java]?.isVisible = visible
                lastVisible[n] = visible
            }
            if (!visible) continue
            val o = n * PoseBuffer.TRANSFORM_STRIDE
            e.components[TransformComponent::class.java]?.apply {
                // The SDK's math types are immutable, so each write allocates small objects.
                setPosition(Vector3(t[o], t[o + 1], t[o + 2]))
                setQuaternion(SdkMath.quat(t[o + 3], t[o + 4], t[o + 5], t[o + 6]))
                setScaleVector(Vector3(t[o + 7], t[o + 8], t[o + 9]))
            }
            val r = a[c]; val g = a[c + 1]; val b = a[c + 2]
            val l = n * 3
            if (abs(r - lastRgb[l]) + abs(g - lastRgb[l + 1]) + abs(b - lastRgb[l + 2]) > COLOR_EPSILON) {
                lastRgb[l] = r; lastRgb[l + 1] = g; lastRgb[l + 2] = b
                val k = Glow.capScale(r, g, b)
                Looks.setGlow(look, pbr[n], unlit[n], r * k, g * k, b * k)
            }
        }
    }

    val materialCount: Int get() = count

    private companion object {
        const val COLOR_EPSILON = 0.01f
    }
}
