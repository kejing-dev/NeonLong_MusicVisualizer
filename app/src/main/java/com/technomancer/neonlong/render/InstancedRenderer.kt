package com.technomancer.neonlong.render

import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.resource.MeshInstancesResource
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Transform
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.SdkMath
import kotlin.math.abs

/**
 * Draws [count] copies of one mesh with GPU instancing: the copies are split into [groups]
 * neighboring groups, each one entity (one draw call) with its own material, holding its
 * members as mesh instances. Every copy keeps its own position, rotation and scale; a group
 * shares one glow color (the average of its members), so color varies along the dragon in
 * steps of one group.
 */
class InstancedRenderer(
    private val name: String,
    private val mesh: MeshResource,
    val look: FamilyRenderer.Look,
    private val texture: TextureResource?,
    val count: Int,
    val groups: Int,
) {
    private val groupOf = IntArray(count) { (it.toLong() * groups / count).toInt() }
    private val ids = Array(count) { "i$it" }
    private val entities = arrayOfNulls<Entity>(groups)
    private val instances = arrayOfNulls<MeshInstancesResource>(groups)
    private val pbr = arrayOfNulls<PhysicallyBasedMaterial>(groups)
    private val unlit = arrayOfNulls<UnlitMaterial>(groups)
    private val sumRgb = FloatArray(groups * 3)
    private val members = IntArray(groups)
    private val lastRgb = FloatArray(groups * 3) { -1f }
    /** Copies already hidden in the renderer; they cost nothing until they're shown again. */
    private val hidden = BooleanArray(count)

    /** False if instancing couldn't be set up; nothing is drawn then (logged). */
    var ok = false
        private set

    val entityList: List<Entity> get() = entities.filterNotNull()

    fun build(parent: Entity) {
        try {
            for (g in 0 until groups) {
                val material = Looks.material(look, texture)
                pbr[g] = material as? PhysicallyBasedMaterial
                unlit[g] = material as? UnlitMaterial
                val list = ArrayList<MeshInstancesResource.Instance>()
                for (n in 0 until count) if (groupOf[n] == g) list += MeshInstancesResource.Instance(ids[n], Transform())
                val res = MeshInstancesResource.create("${name}_$g", list)
                res.toGlobal()
                val model = ModelComponent(mesh, material).apply { meshInstances = res }
                val e = Entity().apply {
                    setName("${name}_$g")
                    components.set(model)
                }
                parent.addChild(e)
                entities[g] = e
                instances[g] = res
            }
            ok = true
        } catch (e: Throwable) {
            Log.e(LOG_TAG, "[boot] instancing failed for $name: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** Starts a frame: clears the per-group color sums. */
    fun begin() {
        sumRgb.fill(0f)
        members.fill(0)
    }

    /** Places copy [n]. Hidden copies are shrunk to nothing. */
    fun set(
        n: Int,
        px: Float, py: Float, pz: Float,
        qx: Float, qy: Float, qz: Float, qw: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float, visible: Boolean,
    ) {
        val grp = groupOf[n]
        val res = instances[grp] ?: return
        if (!visible) {
            // Hide it once, then skip it: nothing is sent or allocated for a hidden copy.
            if (hidden[n]) return
            hidden[n] = true
            res.update(ids[n], Transform(Vector3(px, py, pz), SdkMath.quat(0f, 0f, 0f, 1f), Vector3(0f, 0f, 0f)))
            return
        }
        hidden[n] = false
        // The SDK's math types are immutable, so each update allocates small objects.
        res.update(ids[n], Transform(Vector3(px, py, pz), SdkMath.quat(qx, qy, qz, qw), Vector3(sx, sy, sz)))
        sumRgb[grp * 3] += r; sumRgb[grp * 3 + 1] += g; sumRgb[grp * 3 + 2] += b
        members[grp]++
    }

    /** Ends a frame: pushes each group's average glow to its material when it changed. */
    fun end() {
        for (grp in 0 until groups) {
            val m = members[grp]
            if (m == 0) continue
            val r = sumRgb[grp * 3] / m; val g = sumRgb[grp * 3 + 1] / m; val b = sumRgb[grp * 3 + 2] / m
            val l = grp * 3
            if (abs(r - lastRgb[l]) + abs(g - lastRgb[l + 1]) + abs(b - lastRgb[l + 2]) <= 0.01f) continue
            lastRgb[l] = r; lastRgb[l + 1] = g; lastRgb[l + 2] = b
            val k = Glow.capScale(r, g, b)
            Looks.setGlow(look, pbr[grp], unlit[grp], r * k, g * k, b * k)
        }
    }
}
