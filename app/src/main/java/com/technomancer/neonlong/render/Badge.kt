package com.technomancer.neonlong.render

import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3

/**
 * Placeholder for the 龍 badge: a 48 mm tile at the front-bottom edge of the volume.
 * Tapping it will open the control panel; for now the tap is only logged.
 */
object Badge {
    const val NAME = "PH_Badge"
    const val SIZE = 0.048f
    const val THICKNESS = 0.008f

    fun create(frontZ: Float, baseY: Float): Entity {
        val material = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.12f, 0.10f, 0.16f, 1f))
            setEmissiveColor(Color4(1.0f, 0.18f, 0.53f, 1f)) // Kowloon stop0 #ff2e88
            setMetallic(0.5f)
            setRoughness(0.4f)
        }
        return Entity().apply {
            setName(NAME)
            components.set(ModelComponent(MeshResource.createBox(Vector3(SIZE, SIZE, THICKNESS), 0f), material))
            components.set(TransformComponent().apply { setPosition(Vector3(0f, baseY, frontZ)) })
        }
    }
}
