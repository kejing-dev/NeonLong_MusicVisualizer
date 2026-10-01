package com.technomancer.neonlong.render

import android.util.Log
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.EntityRegistry
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.SdkMath
import com.technomancer.neonlong.ecs.components.SegmentTouchComponent
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.config.SceneConfig
import com.technomancer.neonlong.logic.dragon.BodyLayout
import com.technomancer.neonlong.logic.dragon.ChunkChain
import com.technomancer.neonlong.logic.dragon.PartFamily
import com.technomancer.neonlong.logic.dragon.PoseBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * The dragon volume's entities, created once per container open. DragonRoot converts model
 * units to meters and carries the turntable yaw; the body parts are its children, in model
 * units. The turntable and badge sit in volume space (meters).
 */
class DragonEntities {
    var root: Entity? = null
        private set
    var turntable: Entity? = null
        private set
    var badge: Entity? = null
        private set
    private var lightRig: Entity? = null

    private var families: List<FamilyRenderer> = emptyList()
    private var core: ChainRenderer? = null
    val coreChain = ChunkChain(CORE_CHUNKS)

    /** Width and yaw the root was last placed for. */
    var appliedWidth = -1f
    var appliedYaw = Float.NaN

    val built: Boolean get() = root != null

    /** Materials in use (one per drawn instance). */
    var materialCount = 0
        private set

    /** Builds DragonRoot with every body family, the turntable and the badge. */
    fun build(scene: SceneConfig, lib: MeshLibrary, registry: EntityRegistry): List<Entity> {
        val rootEntity = Entity().apply {
            setName(ROOT)
            components.set(TransformComponent())
        }
        val dorsal = FamilyRenderer(PartFamily.SEG_DORSAL, lib.block, FamilyRenderer.Look.METAL, lib.facade)
        families = listOf(
            dorsal,
            FamilyRenderer(PartFamily.SEG_TIER, lib.block, FamilyRenderer.Look.METAL, lib.facade),
            FamilyRenderer(PartFamily.SEG_BELLY, lib.block, FamilyRenderer.Look.METAL, lib.facade),
            FamilyRenderer(PartFamily.SIDE_PLATE, lib.block, FamilyRenderer.Look.METAL, lib.facade),
            FamilyRenderer(PartFamily.FRAME_RING, lib.squareRing, FamilyRenderer.Look.NEON),
            FamilyRenderer(PartFamily.BELLY_LIGHT, lib.block, FamilyRenderer.Look.NEON),
            FamilyRenderer(PartFamily.TOWER_BODY, lib.tower, FamilyRenderer.Look.TOWER, lib.facade),
            FamilyRenderer(PartFamily.SPIRE, lib.cylBase, FamilyRenderer.Look.NEON),
            FamilyRenderer(PartFamily.CAP, lib.block, FamilyRenderer.Look.NEON),
            FamilyRenderer(PartFamily.BEACON, lib.ball, FamilyRenderer.Look.NEON),
        )
        for (f in families) {
            if (f === dorsal) {
                // Dorsal blocks are what the user touches to send ripples.
                f.build(rootEntity) { n, e ->
                    e.components.set(touchable(Vector3(1f, 1f, 1f)))
                    e.components.set(InteractableComponent())
                    e.components.set(HoverEffectComponent())
                    e.components.set(SegmentTouchComponent(n))
                }
            } else {
                f.build(rootEntity)
            }
        }
        core = ChainRenderer("CORE", coreChain, lib.cylBase).also { it.build(rootEntity) }

        // Metal parts reflect the temporary night environment.
        val lights = Lighting.create(lib.nightEnvironment)
        for (f in families) {
            if (f.look == FamilyRenderer.Look.NEON) continue
            for (e in f.entities) e?.components?.set(Lighting.receiver(lights))
        }
        lightRig = lights
        materialCount = families.sumOf { it.materialCount } + CORE_CHUNKS
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

        val parts = families.sumOf { it.count } + CORE_CHUNKS
        Log.i(LOG_TAG, "[boot] dragon built: $parts body entities, $materialCount materials, turntable, badge")
        return listOf(rootEntity, lights, ring, badgeEntity)
    }

    /** Copies the simulation's output onto the rigid body parts. */
    fun applyParts(pose: PoseBuffer) {
        for (f in families) f.apply(pose)
    }

    /**
     * Copies the energy core onto its chain of pieces. Only the stretch between the first and
     * last body segments is drawn, so the core doesn't stick out bare past the body.
     */
    fun applyCore(pose: PoseBuffer) {
        val fiber = pose.fibers.core
        val steps = fiber.points - 1
        coreChain.update(fiber, (CORE_START_U * steps).toInt(), (CORE_END_U * steps).toInt())
        core?.apply()
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
        registry.remove(ROOT)
        registry.remove(Turntable.NAME)
        registry.remove(Badge.NAME)
        root?.destroy(true)
        turntable?.destroy(true)
        badge?.destroy(true)
        lightRig?.destroy(true)
        lightRig = null
        root = null
        turntable = null
        badge = null
        families = emptyList()
        core = null
        Log.i(LOG_TAG, "[boot] dragon entities destroyed")
    }

    private fun touchable(size: Vector3) =
        CollisionComponent(listOf(ShapeResource.createBox(size)), PhysicsMaterialResource())

    companion object {
        const val ROOT = "DragonRoot"
        /** Energy core pieces while it's drawn as a chain; short pieces keep bends smooth. */
        const val CORE_CHUNKS = 80
        /** Spine range the core is drawn over: from the center of the first body segment to the center of the last, so both ends stay hidden inside blocks. */
        val CORE_START_U = BodyLayout.segmentU(0)
        val CORE_END_U = BodyLayout.segmentU(DragonConfig.SEGMENTS - 1)
    }
}
