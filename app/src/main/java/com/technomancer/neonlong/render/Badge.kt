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
 * ring, with the glowing glyph on its front face. Tapping it opens or closes the control panel.
 */
object Badge {
    const val NAME = "PH_Badge"
    const val SIZE = 0.048f
    /** Thick enough to cover the ring's 16 mm tube where it passes through. */
    const val THICKNESS = 0.02f
    const val TOUCH_DEPTH = 0.05f

    /** The glyph fills this share of the tile's face. */
    private const val GLYPH_FRAC = 0.8f

    /** Glyph color, Kowloon stop0 #ff2e88, at the steady brightness. */
    const val GLOW_R = 1.0f
    const val GLOW_G = 0.18f
    const val GLOW_B = 0.53f

    /** The badge entity and the glyph's material, which pulses. */
    class Parts(val entity: Entity, val glyphMaterial: UnlitMaterial)

    /**
     * Touch area in the badge's own (scaled) units: the full face, and deeper than the tile so
     * it reaches past the ring's grab area.
     */
    val touchSize get() = Vector3(1f, 1f, TOUCH_DEPTH / THICKNESS)

    /**
     * The badge is the unit GEN_Block [block] scaled to 48 × 48 × 20 mm; the glyph child is
     * placed and sized in those scaled units.
     */
    fun create(frontZ: Float, baseY: Float, block: MeshResource, glyphPlate: MeshResource, glyph: TextureResource?): Parts {
        val tile = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.063f, 0.055f, 0.094f, 1f)) // #100e18, like the body metal
            setEmissiveColor(Color4(0.25f, 0.045f, 0.13f, 1f)) // a faint pink rim glow
            setMetallic(0.75f)
            setRoughness(0.33f)
        }
        val material = Looks.material(Look.GLYPH, glyph) as UnlitMaterial
        material.setBaseColor(Color4(GLOW_R, GLOW_G, GLOW_B, 1f))
        val face = Entity().apply {
            setName("${NAME}_Glyph")
            components.set(ModelComponent(glyphPlate, material))
            components.set(TransformComponent().apply {
                // Just in front of the tile's face (0.6 mm), covering [GLYPH_FRAC] of it.
                setPosition(Vector3(0f, 0f, 0.5f + 0.0006f / THICKNESS))
                setScaleVector(Vector3(GLYPH_FRAC, GLYPH_FRAC, 1f))
            })
        }
        val entity = Entity().apply {
            setName(NAME)
            components.set(ModelComponent(block, tile))
            components.set(TransformComponent().apply {
                setPosition(Vector3(0f, baseY, frontZ))
                setScaleVector(Vector3(SIZE, SIZE, THICKNESS))
            })
            addChild(face)
        }
        return Parts(entity, material)
    }
}
