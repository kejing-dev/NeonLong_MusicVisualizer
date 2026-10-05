package com.technomancer.neonlong.render

import android.util.Log
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.PointLightComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.EntityRegistry
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.SdkMath
import com.technomancer.neonlong.ecs.components.SegmentTouchComponent
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.config.GlowConfig
import com.technomancer.neonlong.logic.config.SceneConfig
import com.technomancer.neonlong.logic.config.UiConfig
import com.technomancer.neonlong.logic.dragon.BodyLayout
import com.technomancer.neonlong.logic.dragon.DragonSimulation
import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.dragon.Fibers
import com.technomancer.neonlong.render.FamilyRenderer.Look
import com.technomancer.neonlong.logic.dragon.PartFamily
import com.technomancer.neonlong.logic.dragon.PoseBuffer
import kotlinx.coroutines.yield
import kotlin.math.PI
import kotlin.math.abs
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
    private var badgeGlyph: UnlitMaterial? = null
    private var badgeGain = -1f
    private var lightRig: Entity? = null

    /** Colored lights riding the body, one per entry of [GlowConfig.bodyLightU], and the pearl's light. */
    private val bodyLights = ArrayList<Entity>()
    private var pearlLight: Entity? = null
    private var glowConfig: GlowConfig? = null
    private val lightFrame = Basis()
    private val lightColor = Color3()

    private var parts: List<PartRenderer> = emptyList()
    private var tubes: List<PartRenderer> = emptyList()

    /** Width and yaw the root was last placed for. */
    var appliedWidth = -1f
    var appliedYaw = Float.NaN

    val built: Boolean get() = root != null

    /** Draw calls and materials in use (one per entity with a mesh). */
    var materialCount = 0
        private set

    /** Builds DragonRoot with every body family, the turntable and the badge. */
    suspend fun build(scene: SceneConfig, glow: GlowConfig, lib: MeshLibrary, registry: EntityRegistry, fibers: Fibers): List<Entity> {
        Glow.boost = glow.boost
        glowConfig = glow
        val rootEntity = Entity().apply {
            setName(ROOT)
            components.set(TransformComponent())
        }
        // Dorsal blocks keep one entity each: they're what the user touches and hovers.
        val dorsal = SingleFamily(FamilyRenderer(PartFamily.SEG_DORSAL, lib.block, Look.METAL, lib.bodySkin)) { n, e ->
            e.components.set(touchable(Vector3(1f, 1f, 1f)))
            e.components.set(InteractableComponent())
            e.components.set(HoverEffectComponent())
            e.components.set(SegmentTouchComponent(n))
        }
        // Repeated parts are instanced in groups (one draw call each). Every piece is still drawn
        // with its own position, size and rotation; a group shares one glow color, about one
        // color step per body segment, per tail ribbon and per flame.
        parts = listOf(
            dorsal,
            InstancedFamily(PartFamily.SEG_TIER, lib.block, Look.METAL, lib.bodySkin, groups = 27),
            InstancedFamily(PartFamily.SEG_BELLY, lib.block, Look.METAL, lib.bodySkin, groups = 27),
            InstancedFamily(PartFamily.SIDE_PLATE, lib.block, Look.METAL, lib.bodySkin, groups = 54),
            InstancedFamily(PartFamily.FRAME_RING, lib.squareRing, Look.NEON, null, groups = 27),
            InstancedFamily(PartFamily.BELLY_LIGHT, lib.block, Look.NEON, null, groups = 36),
            InstancedFamily(PartFamily.TOWER_BODY, lib.tower, Look.TOWER, lib.facade, groups = 81),
            InstancedFamily(PartFamily.SPIRE, lib.cylBase, Look.NEON, null, groups = 27),
            InstancedFamily(PartFamily.CAP, lib.block, Look.NEON, null, groups = 27),
            InstancedFamily(PartFamily.BEACON, lib.ball, Look.NEON, null, groups = 81),
            InstancedFamily(PartFamily.PISTON_SLEEVE, lib.cylBase, Look.CHROME, null, groups = 2),
            InstancedFamily(PartFamily.PISTON_ROD, lib.cylBase, Look.CHROME, null, groups = 2),
            SingleFamily(FamilyRenderer(PartFamily.HEAD, lib.headZones[0], Look.METAL, lib.bodySkin, meshes = lib.headZones)),
            SingleFamily(FamilyRenderer(PartFamily.JAW, lib.jaw, Look.METAL, lib.bodySkin)),
            SingleFamily(FamilyRenderer(PartFamily.EYE, lib.block, Look.NEON)),
            SingleFamily(FamilyRenderer(PartFamily.EYE_BEZEL, lib.block, Look.NEON)),
            SingleFamily(FamilyRenderer(PartFamily.GLYPH, lib.glyphPlate, Look.GLYPH, lib.glyph)),
            SingleFamily(FamilyRenderer(PartFamily.ANTLERS, lib.antlers, Look.NEON)),
            SingleFamily(FamilyRenderer(PartFamily.MOUTH_GLOW, lib.ball, Look.ADDITIVE)),
            InstancedFamily(PartFamily.WHISKER_DOT, lib.block, Look.NEON, null, groups = 30),
            InstancedFamily(PartFamily.MANE_BLADE, lib.blade, Look.NEON, null, groups = 24),
            InstancedFamily(PartFamily.LIMB, lib.limb, Look.ARMOUR, lib.armour, groups = 8),
            InstancedFamily(PartFamily.JOINT_RING, lib.torus, Look.NEON, null, groups = 2),
            InstancedFamily(PartFamily.JOINT_BALL, lib.ball, Look.CHROME, null, groups = 1),
            InstancedFamily(PartFamily.PALM, lib.block, Look.ARMOUR, lib.armour, groups = 1),
            InstancedFamily(PartFamily.TOE, lib.limb, Look.ARMOUR, lib.armour, groups = 4),
            InstancedFamily(PartFamily.TALON, lib.talon, Look.NEON, null, groups = 4),
            SingleFamily(FamilyRenderer(PartFamily.PEARL_CORE, lib.pearlCore, Look.NEON)),
            SingleFamily(FamilyRenderer(PartFamily.PEARL_SHELL, lib.pearlShell, Look.GLASS)),
            SingleFamily(FamilyRenderer(PartFamily.PEARL_RING, lib.pearlRing, Look.NEON)),
            InstancedFamily(PartFamily.BREATH, lib.block, Look.ADDITIVE, null, groups = 32),
            // Soft halos stand in for bloom around the brightest parts.
            HaloFamily(PartFamily.BEACON, lib.haloCards, lib.halo, groups = 27, glow.beaconHaloSize, glow.beaconHaloGain, glow.haloMinScale),
            HaloFamily(PartFamily.BELLY_LIGHT, lib.haloCards, lib.halo, groups = 12, glow.bellyHaloSize, glow.bellyHaloGain, glow.haloMinScale),
            HaloFamily(PartFamily.JOINT_RING, lib.haloCards, lib.halo, groups = 2, glow.jointHaloSize, glow.jointHaloGain, glow.haloMinScale),
            HaloFamily(PartFamily.EYE, lib.haloCards, lib.halo, groups = 1, glow.eyeHaloSize, glow.eyeHaloGain, glow.haloMinScale),
            HaloFamily(PartFamily.PEARL_CORE, lib.haloCards, lib.halo, groups = 1, glow.pearlHaloSize, glow.pearlHaloGain, glow.haloMinScale),
        )
        val f = fibers
        tubes = listOf(
            InstancedChains("CORE", arrayOf(f.core), CORE_CHUNKS, lib.cylBase, false, groups = 54, startFrac = CORE_START_U, endFrac = CORE_END_U),
            // A wider, fainter copy of the core that adds light: its glow.
            InstancedChains(
                "CORE_SLEEVE", arrayOf(f.core), CORE_CHUNKS, lib.cylBase, true, groups = 18,
                startFrac = CORE_START_U, endFrac = CORE_END_U, radiusMul = glow.coreSleeveRadius, gain = glow.coreSleeveGain,
            ),
            InstancedChains("TUFT", f.tufts, 4, lib.cylBase, true, groups = 20),
            InstancedChains("TAIL", f.tail, 6, lib.cylBase, true, groups = 36),
            InstancedChains("PEARL_FLAME", f.pearlFlames, 4, lib.cylBase, true, groups = 14),
            InstancedChains("CABLE", f.cables, 12, lib.cylBase, false, groups = 4),
        )
        for (p in parts + tubes) {
            p.build(rootEntity)
            yield() // keep the main thread responsive while building
        }

        // Colored lights that ride the body and the pearl, so the dark metal picks up the neon.
        try {
            for (i in glow.bodyLightU.indices) {
                bodyLights += Entity().apply {
                    setName("BodyLight_$i")
                    components.set(PointLightComponent(Color4(1f, 1f, 1f, 1f), glow.bodyLightIntensity, glow.bodyLightRadius))
                }.also { rootEntity.addChild(it) }
            }
            pearlLight = Entity().apply {
                setName("PearlLight")
                components.set(PointLightComponent(Color4(1f, 1f, 1f, 1f), glow.pearlLightIntensity, glow.pearlLightRadius))
            }.also { rootEntity.addChild(it) }
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "[boot] body lights unavailable: ${e.javaClass.simpleName}: ${e.message}")
        }

        // Metal parts reflect the night environment.
        val lights = Lighting.create(lib.nightEnvironment)
        for (p in parts) for (e in p.metalEntities) e.components.set(Lighting.receiver(lights))
        lightRig = lights
        materialCount = (parts + tubes).sumOf { it.drawCount }
        registry.put(ROOT, rootEntity)
        root = rootEntity
        appliedWidth = -1f
        appliedYaw = Float.NaN

        // Ring height: just high enough that the badge centered on it stays inside the volume.
        val baseY = -scene.defaultHeight * 0.5f + Badge.SIZE * 0.5f + 0.002f
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

        // Centered on the front of the ring, like a buckle the ring runs through.
        val badgeParts = Badge.create(frontZ = rz, baseY = baseY, block = lib.block, glyphPlate = lib.glyphPlate, glyph = lib.glyph)
        badgeGlyph = badgeParts.glyphMaterial
        val badgeEntity = badgeParts.entity.apply {
            // Deeper than the tile so it reaches past the ring's grab area and gets the tap first.
            components.set(touchable(Badge.touchSize))
            components.set(InteractableComponent())
            components.set(HoverEffectComponent())
        }
        registry.put(Badge.NAME, badgeEntity)
        badge = badgeEntity

        Log.i(LOG_TAG, "[boot] dragon built: $materialCount draw calls and materials (instanced), turntable, badge")
        return listOf(rootEntity, lights, ring, badgeEntity)
    }

    /** Copies the simulation's output onto the rigid body parts. */
    fun applyParts(pose: PoseBuffer) {
        for (p in parts) p.apply(pose)
    }

    /**
     * Copies every tube (core, tufts, tail fan, pearl flames, cables) onto its chain of
     * pieces. The core is only drawn between the first and last body segments, so it
     * doesn't stick out bare past the body.
     */
    fun applyFibers(pose: PoseBuffer) {
        for (t in tubes) t.apply(pose)
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
        // Light reach is set in meters, so it follows the dragon's size.
        if (width != appliedWidth) glowConfig?.let { glow ->
            for (l in bodyLights) l.components[PointLightComponent::class.java]?.attenuationRadius = glow.bodyLightRadius * k
            pearlLight?.components?.get(PointLightComponent::class.java)?.attenuationRadius = glow.pearlLightRadius * k
        }
        appliedWidth = width
        appliedYaw = yaw
        return k
    }

    /** Index of [entity] if it is one of the body segments, else −1. */
    fun segmentIndexOf(entity: Entity?): Int =
        entity?.components?.get(SegmentTouchComponent::class.java)?.segment ?: -1

    /** Destroys everything built by [build]. */
    /**
     * Moves the body lights along the spine and tints them with the palette color at their
     * position; the pearl's light follows the pearl in the accent color.
     */
    fun updateLights(sim: DragonSimulation, pose: PoseBuffer) {
        val glow = glowConfig ?: return
        for (i in bodyLights.indices) {
            val u = glow.bodyLightU[i]
            val f = sim.spine.frameAt(u, lightFrame)
            sim.palette.palAt(u, lightColor)
            bodyLights[i].components[TransformComponent::class.java]?.setPosition(Vector3(f.p.x, f.p.y, f.p.z))
            bodyLights[i].components[PointLightComponent::class.java]?.color = Color4(lightColor.r, lightColor.g, lightColor.b, 1f)
        }
        val pearl = pearlLight ?: return
        val t = pose.transforms(PartFamily.PEARL_CORE)
        val a = sim.palette.accent
        pearl.components[TransformComponent::class.java]?.setPosition(Vector3(t[0], t[1], t[2]))
        pearl.components[PointLightComponent::class.java]?.color = Color4(a.r, a.g, a.b, 1f)
    }

    /**
     * Pulses the badge glyph at [UiConfig.badgePulseHz] with [UiConfig.badgePulseGain] while
     * [pulsing]; otherwise holds it steady. [timeS] is the simulation time.
     */
    fun updateBadge(timeS: Float, pulsing: Boolean, ui: UiConfig) {
        val m = badgeGlyph ?: return
        val gain = if (pulsing) 1f + ui.badgePulseGain * sin(2f * PI.toFloat() * ui.badgePulseHz * timeS) else 1f
        if (abs(gain - badgeGain) < 0.01f) return
        badgeGain = gain
        // Scaled so the brightest point of the pulse is the full glyph color.
        val k = gain / (1f + ui.badgePulseGain)
        m.setBaseColor(Color4(Badge.GLOW_R * k, Badge.GLOW_G * k, Badge.GLOW_B * k, 1f))
    }

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
        badgeGlyph = null
        bodyLights.clear()
        pearlLight = null
        badgeGain = -1f
        parts = emptyList()
        tubes = emptyList()
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
