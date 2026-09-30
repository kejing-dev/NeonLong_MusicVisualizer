package com.technomancer.neonlong.render

import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshModel
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Placeholder turntable: a thin ring lying flat at the base of the volume. Built as a
 * runtime mesh (the SDK's torus primitive doesn't document which radius is which).
 * Dragging it to rotate the dragon comes later.
 */
object Turntable {
    const val NAME = "PH_Turntable"

    /**
     * An oval ring lying flat at [baseY], [radiusX] across and [radiusZ] deep, so it follows
     * the volume's footprint without poking out the front or back.
     */
    fun create(radiusX: Float, radiusZ: Float, tube: Float, baseY: Float): Entity {
        val material = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.08f, 0.08f, 0.10f, 1f))
            setEmissiveColor(Color4(0.09f, 0.9f, 1.0f, 1f)) // Kowloon stop2 #18e7ff
            setMetallic(0.8f)
            setRoughness(0.3f)
        }
        return Entity().apply {
            setName(NAME)
            components.set(ModelComponent(ringMesh(radiusX, radiusZ, tube), material))
            components.set(TransformComponent().apply { setPosition(Vector3(0f, baseY, 0f)) })
        }
    }

    /** Tube of radius [minor] swept around an ellipse in the XZ plane with radii [rx] and [rz]. */
    private fun ringMesh(rx: Float, rz: Float, minor: Float, around: Int = 64, sides: Int = 8): MeshResource {
        val positions = ArrayList<Vector3>((around + 1) * (sides + 1))
        val normals = ArrayList<Vector3>((around + 1) * (sides + 1))
        for (i in 0..around) {
            val a = (2.0 * PI * i / around).toFloat()
            val ca = cos(a); val sa = sin(a)
            // Outward normal of the ellipse at this point, in the XZ plane.
            var nx = ca / rx; var nz = sa / rz
            val len = sqrt(nx * nx + nz * nz)
            nx /= len; nz /= len
            for (j in 0..sides) {
                val b = (2.0 * PI * j / sides).toFloat()
                val cb = cos(b); val sb = sin(b)
                positions += Vector3(rx * ca + minor * cb * nx, minor * sb, rz * sa + minor * cb * nz)
                normals += Vector3(cb * nx, sb, cb * nz)
            }
        }
        val indices = ArrayList<Int>(around * sides * 6)
        val stride = sides + 1
        for (i in 0 until around) {
            for (j in 0 until sides) {
                val p0 = i * stride + j
                val p1 = (i + 1) * stride + j
                // Two triangles per quad, wound so faces point outward.
                indices += p0; indices += p0 + 1; indices += p1
                indices += p1; indices += p0 + 1; indices += p1 + 1
            }
        }
        return MeshResource.createWithMeshModel(MeshModel(positions, indices, normals), name = NAME)
    }
}
