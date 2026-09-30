package com.technomancer.neonlong.render

import android.util.Log
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.EntityRegistry
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.SdkMath
import com.technomancer.neonlong.ecs.components.SegmentTouchComponent
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.config.SceneConfig
import kotlin.math.cos
import kotlin.math.sin

/**
 * The dragon volume's entities, created once per container open and looked up by index
 * each frame. DragonRoot converts model units to meters and carries the turntable yaw;
 * its children are in model units. The turntable and badge sit in volume space (meters).
 */
class DragonEntities {
    var root: Entity? = null
        private set
    var turntable: Entity? = null
        private set
    var badge: Entity? = null
        private set
    val segments = arrayOfNulls<Entity>(DragonConfig.SEGMENTS)
    val segmentMaterials = arrayOfNulls<PhysicallyBasedMaterial>(DragonConfig.SEGMENTS)

    /** Width and yaw the root was last placed for. */
    var appliedWidth = -1f
    var appliedYaw = Float.NaN

    val built: Boolean get() = root != null

    /** Builds DragonRoot with 27 touchable gray-box segments, the turntable and the badge. */
    fun build(scene: SceneConfig, registry: EntityRegistry): List<Entity> {
        val unitBox = MeshResource.createBox(Vector3(1f, 1f, 1f), 0f)
        val rootEntity = Entity().apply {
            setName(ROOT)
            components.set(TransformComponent())
        }
        for (i in 0 until DragonConfig.SEGMENTS) {
            val name = segmentName(i)
            // One material per segment so each can glow on its own.
            val material = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
                setBaseColor(Color4(0.30f, 0.30f, 0.33f, 1f))
                setMetallic(0.6f)
                setRoughness(0.4f)
            }
            val seg = Entity().apply {
                setName(name)
                components.set(ModelComponent(unitBox, material))
                components.set(TransformComponent())
                components.set(touchable(Vector3(1f, 1f, 1f)))
                components.set(InteractableComponent())
                components.set(HoverEffectComponent())
                components.set(SegmentTouchComponent(i))
            }
            rootEntity.addChild(seg)
            segments[i] = seg
            segmentMaterials[i] = material
            registry.put(name, seg)
        }
        registry.put(ROOT, rootEntity)
        root = rootEntity
        appliedWidth = -1f
        appliedYaw = Float.NaN

        val baseY = -scene.defaultHeight * 0.5f + 0.01f
        val rx = scene.turntableRadiusFrac * scene.defaultWidth
        val rz = scene.turntableRadiusFrac * scene.defaultDepth
        val ring = Turntable.create(radiusX = rx, radiusZ = rz, tube = 0.008f, baseY = baseY).apply {
            // A flat grab area covering the ring; thin so it doesn't block the dragon above.
            components.set(touchable(Vector3(2f * rx + 0.04f, 0.03f, 2f * rz + 0.04f)))
            components.set(InteractableComponent())
            components.set(HoverEffectComponent())
        }
        registry.put(Turntable.NAME, ring)
        turntable = ring

        val badgeEntity = Badge.create(frontZ = scene.defaultDepth * 0.5f - 0.04f, baseY = baseY + 0.03f).apply {
            components.set(touchable(Vector3(Badge.SIZE, Badge.SIZE, Badge.THICKNESS)))
            components.set(InteractableComponent())
            components.set(HoverEffectComponent())
        }
        registry.put(Badge.NAME, badgeEntity)
        badge = badgeEntity

        Log.i(LOG_TAG, "[boot] dragon entities built: root + ${DragonConfig.SEGMENTS} touchable segments + turntable + badge")
        return listOf(rootEntity, ring, badgeEntity)
    }

    /**
     * Places DragonRoot for a volume [width] meters wide and a turntable [yaw] (radians,
     * counter-clockwise from above): scale width / 36 and the model box centered on the
     * volume's origin, turned about the vertical axis through that center.
     */
    fun place(width: Float, yaw: Float, scene: SceneConfig, dragon: DragonConfig): Float {
        val k = width / scene.modelUnitsAcross
        val cx = -dragon.boundsCenterX * k
        val cy = -dragon.boundsCenterY * k
        val cz = -dragon.boundsCenterZ * k
        val c = cos(yaw)
        val s = sin(yaw)
        root?.components?.get(TransformComponent::class.java)?.apply {
            setScaleVector(Vector3(k, k, k))
            setQuaternion(SdkMath.quat(0f, sin(yaw * 0.5f), 0f, cos(yaw * 0.5f)))
            // Rotate the centering offset too, so the dragon turns about the volume's center.
            setPosition(Vector3(c * cx + s * cz, cy, -s * cx + c * cz))
        }
        appliedWidth = width
        appliedYaw = yaw
        return k
    }

    /** Index of [entity] if it is one of the body segments, else −1. */
    fun segmentIndexOf(entity: Entity?): Int =
        entity?.components?.get(SegmentTouchComponent::class.java)?.segment ?: -1

    /** Destroys everything built by [build]. */
    fun destroy(registry: EntityRegistry) {
        for (i in segments.indices) {
            registry.remove(segmentName(i))
            segments[i] = null
            segmentMaterials[i] = null
        }
        registry.remove(ROOT)
        registry.remove(Turntable.NAME)
        registry.remove(Badge.NAME)
        root?.destroy(true)
        turntable?.destroy(true)
        badge?.destroy(true)
        root = null
        turntable = null
        badge = null
        Log.i(LOG_TAG, "[boot] dragon entities destroyed")
    }

    private fun touchable(size: Vector3) =
        CollisionComponent(listOf(ShapeResource.createBox(size)), PhysicsMaterialResource())

    companion object {
        const val ROOT = "DragonRoot"
        fun segmentName(i: Int): String = "PH_Segment_%02d".format(i)
    }
}
