package com.technomancer.neonlong.render

import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.EntityRegistry
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.config.SceneConfig

/**
 * The dragon volume's entities, created once per container open and looked up by index
 * each frame. DragonRoot converts model units to meters; its children are in model units.
 */
class DragonEntities {
    var root: Entity? = null
        private set
    var turntable: Entity? = null
        private set
    val segments = arrayOfNulls<Entity>(DragonConfig.SEGMENTS)

    /** Width the root's scale was last set for, in meters. */
    var appliedWidth = -1f

    val built: Boolean get() = root != null

    /** Builds DragonRoot with 27 gray-box segments, plus the turntable. Returns the top-level entities. */
    fun build(scene: SceneConfig, dragon: DragonConfig, registry: EntityRegistry): List<Entity> {
        val material = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.30f, 0.30f, 0.33f, 1f))
            setMetallic(0.6f)
            setRoughness(0.4f)
        }
        val unitBox = MeshResource.createBox(Vector3(1f, 1f, 1f), 0f)
        val rootEntity = Entity().apply {
            setName(ROOT)
            components.set(TransformComponent())
        }
        for (i in 0 until DragonConfig.SEGMENTS) {
            val name = segmentName(i)
            val seg = Entity().apply {
                setName(name)
                components.set(ModelComponent(unitBox, material))
                components.set(TransformComponent())
            }
            rootEntity.addChild(seg)
            segments[i] = seg
            registry.put(name, seg)
        }
        registry.put(ROOT, rootEntity)
        root = rootEntity
        appliedWidth = -1f

        val ring = Turntable.create(
            radiusX = scene.turntableRadiusFrac * scene.defaultWidth,
            radiusZ = scene.turntableRadiusFrac * scene.defaultDepth,
            tube = 0.008f,
            baseY = -scene.defaultHeight * 0.5f + 0.01f,
        )
        registry.put(Turntable.NAME, ring)
        turntable = ring
        Log.i(LOG_TAG, "[boot] dragon entities built: root + ${DragonConfig.SEGMENTS} segments + turntable")
        return listOf(rootEntity, ring)
    }

    /**
     * Scales DragonRoot so the model box fills a volume [width] meters wide, and centers the
     * model box's center on the volume's origin. Returns meters per model unit.
     */
    fun applyWidth(width: Float, scene: SceneConfig, dragon: DragonConfig): Float {
        val k = width / scene.modelUnitsAcross
        root?.components?.get(TransformComponent::class.java)?.apply {
            setScaleVector(Vector3(k, k, k))
            setPosition(Vector3(-dragon.boundsCenterX * k, -dragon.boundsCenterY * k, -dragon.boundsCenterZ * k))
        }
        appliedWidth = width
        return k
    }

    /** Destroys everything built by [build]. */
    fun destroy(registry: EntityRegistry) {
        for (i in segments.indices) {
            registry.remove(segmentName(i))
            segments[i] = null
        }
        registry.remove(ROOT)
        registry.remove(Turntable.NAME)
        root?.destroy(true)
        turntable?.destroy(true)
        root = null
        turntable = null
        Log.i(LOG_TAG, "[boot] dragon entities destroyed")
    }

    companion object {
        const val ROOT = "DragonRoot"
        fun segmentName(i: Int): String = "PH_Segment_%02d".format(i)
    }
}
