package com.technomancer.neonlong.render

import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.render.FamilyRenderer.Look

/**
 * The 龍 badge: a dark 48 mm metal tile at the front-bottom edge of the volume, centered on the
 * ring, with the glowing glyph on its front face. Tapping it plays or pauses the demo for now.
 */
object Badge {
    const val NAME = "PH_Badge"
    const val SIZE = 0.048f
    /** Thick enough to cover the ring's 16 mm tube where it passes through. */
    const val THICKNESS = 0.02f
    const val TOUCH_DEPTH = 0.05f

    /** The glyph fills this share of the tile's face. */
    private const val GLYPH_FRAC = 0.8f

    fun create(frontZ: Float, baseY: Float, glyphPlate: MeshResource, glyph: TextureResource?): Entity {
        val tile = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.063f, 0.055f, 0.094f, 1f)) // #100e18, like the body metal
            setEmissiveColor(Color4(0.25f, 0.045f, 0.13f, 1f)) // a faint pink rim glow
            setMetallic(0.75f)
            setRoughness(0.33f)
        }
        val face = Entity().apply {
            setName("${NAME}_Glyph")
            val material = Looks.material(Look.GLYPH, glyph) as UnlitMaterial
            material.setBaseColor(Color4(1.0f, 0.18f, 0.53f, 1f)) // Kowloon stop0 #ff2e88
            components.set(ModelComponent(glyphPlate, material))
            components.set(TransformComponent().apply {
                // The plate mesh is a 1 m tile; scale it to the face, just in front of the tile.
                setPosition(Vector3(0f, 0f, THICKNESS * 0.5f + 0.0006f))
                setScaleVector(Vector3(SIZE * GLYPH_FRAC, SIZE * GLYPH_FRAC, 0.1f))
            })
        }
        return Entity().apply {
            setName(NAME)
            components.set(ModelComponent(MeshResource.createBox(Vector3(SIZE, SIZE, THICKNESS), 0f), tile))
            components.set(TransformComponent().apply { setPosition(Vector3(0f, baseY, frontZ)) })
            addChild(face)
        }
    }
}
