package com.technomancer.neonlong.render

import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.Material
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.ecs.resource.UnlitMaterial
import com.pico.spatial.core.math.Color4
import com.technomancer.neonlong.render.FamilyRenderer.Look

/** Materials for each look, shared by the per-entity and instanced renderers. */
object Looks {
    fun material(look: Look, texture: TextureResource?): Material = when (look) {
        // Neon is shown at its full color: no tone mapping, which would gray it out.
        Look.NEON -> UnlitMaterial.create(BlendingMode.OPAQUE).apply { setApplyToneMapping(false) }
        Look.ADDITIVE -> UnlitMaterial.create(BlendingMode.ADD).apply {
            setApplyToneMapping(false)
            setDepthWrite(false)
        }
        Look.GLYPH, Look.HALO -> UnlitMaterial.create(BlendingMode.ADD).apply {
            setApplyToneMapping(false)
            setDepthWrite(false)
            if (texture != null) setBaseColorTexture(texture)
        }
        Look.GLASS -> PhysicallyBasedMaterial.create(BlendingMode.TRANSPARENT).apply {
            setBaseColor(Color4(0.133f, 0.133f, 0.2f, 1f)) // #222233
            setMetallic(1f); setRoughness(0.1f); setOpacity(0.35f)
        }
        Look.CHROME -> PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
            setBaseColor(Color4(0.604f, 0.604f, 0.651f, 1f)) // #9a9aa6
            setMetallic(1f); setRoughness(0.18f)
        }
        Look.METAL, Look.TOWER, Look.ARMOUR -> PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
            when (look) {
                Look.METAL -> { setBaseColor(Color4(0.063f, 0.055f, 0.094f, 1f)); setMetallic(0.75f); setRoughness(0.33f) } // #100e18
                Look.TOWER -> { setBaseColor(Color4(0.055f, 0.051f, 0.086f, 1f)); setMetallic(0.7f); setRoughness(0.35f) } // #0e0d16
                else -> { setBaseColor(Color4(0.082f, 0.075f, 0.114f, 1f)); setMetallic(0.85f); setRoughness(0.32f) } // #15131d
            }
            if (texture != null) setEmissiveTexture(texture)
        }
    }

    /** Sets a material's glow: emissive for lit looks, base color for unlit ones; chrome and glass don't glow. */
    fun setGlow(look: Look, pbr: PhysicallyBasedMaterial?, unlit: UnlitMaterial?, r: Float, g: Float, b: Float) {
        if (look != Look.CHROME && look != Look.GLASS) pbr?.setEmissiveColor(Color4(r, g, b, 1f))
        unlit?.setBaseColor(Color4(r, g, b, 1f))
    }
}
