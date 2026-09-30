package com.technomancer.neonlong.debug

import android.util.Log
import com.pico.spatial.core.container.SpatialViewContent
import com.pico.spatial.core.ecs.CollisionComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.HoverEffectComponent
import com.pico.spatial.core.ecs.InteractableComponent
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.AssetBundle
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshModel
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.PhysicsMaterialResource
import com.pico.spatial.core.ecs.resource.ShaderGraphMaterial
import com.pico.spatial.core.ecs.resource.ShapeResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.platform.HandController
import com.pico.spatial.ui.platform.HandControllerHapticType
import com.pico.spatial.ui.platform.SpatialHandControllerHaptic
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.SdkMath
import kotlin.math.cos
import kotlin.math.sin

/**
 * Debug-only engine spike. Measures the per-frame cost of updating the transform and one
 * material color of N separate entities (N = 100, 400, 800, 1200; 600 frames each), then
 * probes runtime mesh updates, unlit/additive materials, ShaderGraph parameters, tap and
 * hover, and controller haptics. Results are logged with the `[spike]` prefix.
 *
 * Start with `adb shell am start -n <package>/<launch activity> --ez nl.spike true`, or
 * `adb shell setprop debug.neonlong.spike 1` before launching.
 */
class EntitySpike : System() {
    override fun update(context: SceneUpdateContext) {
        State.step(context.deltaTime)
    }

    private object State {
        val counts = intArrayOf(100, 400, 800, 1200)
        const val FRAMES = 600
        const val MESH_FRAMES = 120

        var content: SpatialViewContent? = null
        var root: Entity? = null
        var stage = 0
        var frame = 0
        var time = 0f
        var boxes = ArrayList<Entity>()
        var materials = ArrayList<PhysicallyBasedMaterial>()
        val updateNs = LongArray(FRAMES)
        val frameDt = FloatArray(FRAMES)
        var meshProbe: MeshResource? = null
        var meshFrame = 0
        val meshNs = LongArray(MESH_FRAMES)
        var probesDone = false
        var tapTarget: Entity? = null

        fun step(dt: Float) {
            val c = content ?: return
            time += dt
            if (stage < counts.size) {
                runCountStage(c, dt)
            } else if (!probesDone) {
                runProbes(c)
            }
        }

        private fun runCountStage(c: SpatialViewContent, dt: Float) {
            val n = counts[stage]
            if (boxes.isEmpty()) {
                buildBoxes(c, n)
                frame = 0
                return
            }
            val start = java.lang.System.nanoTime()
            for (i in 0 until boxes.size) {
                val phase = time * 1.5f + i * 0.37f
                val x = ((i % 12) - 5.5f) * 0.08f + 0.02f * sin(phase)
                val y = (((i / 12) % 10) - 4.5f) * 0.04f
                val z = ((i / 120) - 5f) * 0.07f + 0.02f * cos(phase)
                boxes[i].components[TransformComponent::class.java]?.apply {
                    setPosition(Vector3(x, y, z))
                    setQuaternion(SdkMath.quat(0f, sin(phase * 0.5f), 0f, cos(phase * 0.5f)))
                }
                val g = 0.5f + 0.5f * sin(phase)
                materials[i].setEmissiveColor(Color4(g, 0.2f, 1f - g, 1f))
            }
            updateNs[frame] = java.lang.System.nanoTime() - start
            frameDt[frame] = dt
            frame++
            if (frame >= FRAMES) {
                report(n)
                destroyBoxes()
                stage++
            }
        }

        private fun buildBoxes(c: SpatialViewContent, n: Int) {
            val r = root ?: Entity().apply {
                setName("PH_SpikeRoot")
                components.set(TransformComponent())
                c.addEntity(this)
            }.also { root = it }
            val mesh = MeshResource.createBox(Vector3(0.03f, 0.03f, 0.03f), 0f)
            for (i in 0 until n) {
                val mat = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
                    setBaseColor(Color4(0.2f, 0.2f, 0.22f, 1f))
                }
                val box = Entity().apply {
                    setName("PH_SpikeBox")
                    components.set(ModelComponent(mesh, mat))
                    components.set(TransformComponent())
                }
                r.addChild(box)
                boxes += box
                materials += mat
            }
            Log.i(LOG_TAG, "[spike] built $n boxes, each with its own material")
        }

        private fun report(n: Int) {
            val sorted = updateNs.copyOf().also { it.sort() }
            val avgMs = updateNs.average() / 1e6
            val p95Ms = sorted[(FRAMES * 95) / 100 - 1] / 1e6
            val dts = frameDt.copyOf().also { it.sort() }
            val medianDt = dts[FRAMES / 2]
            val misses = frameDt.count { it > medianDt * 1.5f }
            Log.i(
                LOG_TAG,
                "[spike] N=$n update avg=%.3fms p95=%.3fms | frame median=%.2fms max=%.2fms misses(>1.5x median)=%d/%d"
                    .format(avgMs, p95Ms, medianDt * 1000f, dts.last() * 1000f, misses, FRAMES),
            )
        }

        private fun destroyBoxes() {
            for (b in boxes) b.destroy(true)
            boxes.clear()
            materials.clear()
        }

        private fun runProbes(c: SpatialViewContent) {
            if (meshFrame == 0) {
                probeOnce("c unlit additive material") {
                    val m = UnlitMaterial.create(BlendingMode.ADD)
                    m.setBaseColor(Color4(1f, 0.2f, 0.6f, 1f))
                    "UnlitMaterial.create(BlendingMode.ADD) ok"
                }
                probeOnce("d ShaderGraph per-instance float") {
                    val bundle = AssetBundle.load("asset://editor-asset.bundle")
                    try {
                        val sg = ShaderGraphMaterial.loadFromAssetBundle(bundle, "SG_SpikeFloat")
                        sg.setParameter("band", 0.5f)
                        "loaded SG_SpikeFloat and set 'band'"
                    } finally {
                        bundle.close()
                    }
                }
                probeOnce("e tap + hover") {
                    val box = Entity().apply {
                        setName("PH_SpikeTapBox")
                        components.set(
                            ModelComponent(
                                MeshResource.createBox(Vector3(0.12f, 0.12f, 0.12f), 0f),
                                PhysicallyBasedMaterial.create(BlendingMode.OPAQUE),
                            ),
                        )
                        components.set(TransformComponent())
                        components.set(
                            CollisionComponent(
                                listOf(ShapeResource.createBox(Vector3(0.12f, 0.12f, 0.12f))),
                                PhysicsMaterialResource(),
                            ),
                        )
                        components.set(InteractableComponent())
                        components.set(HoverEffectComponent())
                    }
                    c.addEntity(box)
                    tapTarget = box
                    "tap box placed at the volume center; tap it and look for '[spike] tap'"
                }
                Log.i(LOG_TAG, "[spike] probe f (hand tracking) deferred until the tracking work")
                Log.i(LOG_TAG, "[spike] probe g (controller buttons) pending: no button-state API found yet")
                Log.i(
                    LOG_TAG,
                    "[spike] probe h (ObjectAudio) pending: needs the generated demo WAV; " +
                        "AudioResource.load(name, Uri, Context) exists for content URIs",
                )
            }
            if (meshFrame < MESH_FRAMES) {
                probeMeshUpdate(c)
                return
            }
            val sorted = meshNs.copyOf().also { it.sort() }
            Log.i(
                LOG_TAG,
                "[spike] probe a runtime mesh (241x12 tube, ${241 * 13} verts) replace avg=%.3fms p95=%.3fms"
                    .format(meshNs.average() / 1e6, sorted[(MESH_FRAMES * 95) / 100 - 1] / 1e6),
            )
            Log.i(LOG_TAG, "[spike] probe b per-entity material color: covered by the N stages above")
            Log.i(LOG_TAG, "[spike] done")
            probesDone = true
        }

        private fun probeMeshUpdate(c: SpatialViewContent) {
            val start = java.lang.System.nanoTime()
            val model = tubeModel(time)
            val mesh = meshProbe
            if (mesh == null) {
                val created = MeshResource.createWithMeshModel(model, name = "PH_SpikeTube")
                c.addEntity(
                    Entity().apply {
                        setName("PH_SpikeTube")
                        components.set(ModelComponent(created, PhysicallyBasedMaterial.create(BlendingMode.OPAQUE)))
                        components.set(TransformComponent().apply { setPosition(Vector3(0f, -0.15f, 0f)) })
                    },
                )
                meshProbe = created
            } else {
                mesh.replaceWithMeshModel(model)
            }
            meshNs[meshFrame] = java.lang.System.nanoTime() - start
            meshFrame++
        }

        /** A wavy tube like the energy core: 241 rings of 12 sides. */
        private fun tubeModel(t: Float): MeshModel {
            val rings = 241
            val sides = 12
            val positions = ArrayList<Vector3>(rings * (sides + 1))
            val indices = ArrayList<Int>((rings - 1) * sides * 6)
            for (i in 0 until rings) {
                val u = i / (rings - 1f)
                val cx = (u - 0.5f) * 0.9f
                val cy = 0.05f * sin(10f * u - 2.3f * t)
                val r = 0.02f * (1f - 0.6f * u)
                for (j in 0..sides) {
                    val a = j * (2f * Math.PI.toFloat() / sides)
                    positions += Vector3(cx, cy + r * cos(a), r * sin(a))
                }
            }
            val stride = sides + 1
            for (i in 0 until rings - 1) {
                for (j in 0 until sides) {
                    val p0 = i * stride + j
                    val p1 = p0 + stride
                    indices += p0; indices += p1; indices += p0 + 1
                    indices += p0 + 1; indices += p1; indices += p1 + 1
                }
            }
            return MeshModel(positions, indices)
        }

        private inline fun probeOnce(name: String, block: () -> String) {
            try {
                Log.i(LOG_TAG, "[spike] probe $name: ${block()}")
            } catch (e: Throwable) {
                Log.i(LOG_TAG, "[spike] probe $name: FAILED ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        fun reset() {
            destroyBoxes()
            root?.destroy(true)
            root = null
            content = null
            stage = 0
            frame = 0
            meshFrame = 0
            meshProbe = null
            probesDone = false
            tapTarget = null
        }
    }

    companion object {
        /** Hands the spike the SpatialView content to build into. */
        fun attach(content: SpatialViewContent) {
            State.reset()
            State.content = content
            Log.i(LOG_TAG, "[spike] starting: N=${State.counts.joinToString()} x ${State.FRAMES} frames")
        }

        fun onTap(target: Entity?, haptic: SpatialHandControllerHaptic) {
            val hit = target != null && target == State.tapTarget
            Log.i(LOG_TAG, "[spike] tap on ${target?.getName()} (probe e ${if (hit) "ok" else "missed the box"})")
            try {
                haptic.feedback(HandControllerHapticType(77, 200, 10), HandController.Right)
                Log.i(LOG_TAG, "[spike] probe i haptics: feedback sent (feel the right controller)")
            } catch (e: Throwable) {
                Log.i(LOG_TAG, "[spike] probe i haptics: FAILED ${e.message}")
            }
        }

        fun destroyAll() = State.reset()
    }
}
