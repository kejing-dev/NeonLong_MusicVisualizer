package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The head: a stack of blocks just ahead of the first spine sample, with a jaw that opens on
 * bass and beats, glowing eyes, the 龍 plate, neon antlers and a mouth glow. Head space has
 * x = side, y = up, z = forward, and is scaled by [SCALE].
 */
class HeadRig {
    /** Head origin and axes (side, up, forward) in model space. */
    val frame = Basis()
    val rot = Quat()
    var jawAngle = 0f
        private set

    private val tmp = Vec3()
    private val q = Quat()
    private val local = Quat()
    private val color = Color3()

    /** Places the head 1.3 units ahead of the spine's first sample, along its frame. */
    fun place(spine: Spine) {
        spine.frameAt(0f, frame)
        frame.p.addScaled(frame.f, 1.3f)
        rot.setFromColumns(frame.s, frame.u, frame.f)
    }

    /** out = model-space point for head-space (x, y, z). */
    fun toModel(x: Float, y: Float, z: Float, out: Vec3): Vec3 {
        val f = frame
        return out.set(
            f.p.x + SCALE * (f.s.x * x + f.u.x * y + f.f.x * z),
            f.p.y + SCALE * (f.s.y * x + f.u.y * y + f.f.y * z),
            f.p.z + SCALE * (f.s.z * x + f.u.z * y + f.f.z * z),
        )
    }

    /** out = model-space direction for head-space (x, y, z) (not rescaled). */
    fun dirToModel(x: Float, y: Float, z: Float, out: Vec3): Vec3 {
        val f = frame
        return out.set(
            f.s.x * x + f.u.x * y + f.f.x * z,
            f.s.y * x + f.u.y * y + f.f.y * z,
            f.s.z * x + f.u.z * y + f.f.z * z,
        )
    }

    /** Jaw opening for the current bass and beat pulse, capped at [JAW_MAX]. */
    fun updateJaw(bass: Float, beatPulse: Float) {
        jawAngle = min(JAW_MAX, 0.4f * bass + 0.32f * beatPulse)
    }

    /**
     * Writes HEAD, JAW, EYE, EYE_BEZEL, GLYPH, ANTLERS and MOUTH_GLOW. [pulse] is the beat
     * pulse already scaled for "Reduce flashing"; [gain] multiplies every glow (whole-dragon flash).
     */
    fun write(
        out: PoseBuffer, palette: Palette,
        bass: Float, mid: Float, high: Float, pulse: Float, gain: Float,
    ) {
        val s = SCALE
        // Head and jaw (single meshes each).
        val blockGain = (0.5f + 1.2f * bass + 0.6f * pulse) * gain
        // One mesh per head color zone, all at the head's pose.
        for (z in 0 until HeadZones.COUNT) {
            out.setTransform(PartFamily.HEAD, z, frame.p.x, frame.p.y, frame.p.z, rot, s, s, s)
            palette.palAt(HeadZones.U[z], color)
            val k = blockGain * HeadZones.GLOW[z]
            out.setAppearance(PartFamily.HEAD, z, color.r * k, color.g * k, color.b * k, true)
        }
        palette.palAt(0.1f, color)

        toModel(JAW_PIVOT_X, JAW_PIVOT_Y, JAW_PIVOT_Z, tmp)
        local.set(sin(jawAngle / 2), 0f, 0f, cos(jawAngle / 2)) // opens downward about the side axis
        q.mul(rot, local)
        out.setTransform(PartFamily.JAW, 0, tmp.x, tmp.y, tmp.z, q, s, s, s)
        out.setAppearance(PartFamily.JAW, 0, color.r * blockGain, color.g * blockGain, color.b * blockGain, true)

        // Eyes and bezels, one per side.
        val a = palette.accent
        val eyeGain = (2f + 3f * pulse + 1.5f * bass) * gain
        val bezelGain = (1.1f + 2f * mid) * gain
        for (k in 0..1) {
            val side = if (k == 0) -1f else 1f
            toModel(1.125f * side, 0.56f, 0.55f, tmp)
            out.setTransform(PartFamily.EYE, k, tmp.x, tmp.y, tmp.z, rot, 0.06f * s, 0.18f * s, 0.52f * s)
            out.setAppearance(PartFamily.EYE, k, a.r * eyeGain, a.g * eyeGain, a.b * eyeGain, true)
            toModel(1.1f * side, 0.56f, 0.55f, tmp)
            local.set(0f, sin(0.075f * side), 0f, cos(0.075f * side))
            q.mul(rot, local)
            out.setTransform(PartFamily.EYE_BEZEL, k, tmp.x, tmp.y, tmp.z, q, 0.05f * s, 0.28f * s, 0.66f * s)
            val c = palette.stop2
            out.setAppearance(PartFamily.EYE_BEZEL, k, c.r * bezelGain, c.g * bezelGain, c.b * bezelGain, true)
        }

        // 龍 plate on the crown, facing up.
        toModel(0f, 1.465f, -0.5f, tmp)
        eulerXYZ(-PI_F / 2, 0f, PI_F, local)
        q.mul(rot, local)
        out.setTransform(PartFamily.GLYPH, 0, tmp.x, tmp.y, tmp.z, q, 0.75f * s, 0.75f * s, 0.75f * s)
        color.lerp(palette.stop0, a, 0.3f)
        val glyphGain = (1.2f + 2f * pulse) * gain
        out.setAppearance(PartFamily.GLYPH, 0, color.r * glyphGain, color.g * glyphGain, color.b * glyphGain, true)

        // Antlers (one mesh for both sides, in head space).
        out.setTransform(PartFamily.ANTLERS, 0, frame.p.x, frame.p.y, frame.p.z, rot, s, s, s)
        color.lerp(palette.stop2, a, min(1f, 0.6f * pulse))
        val antlerGain = (0.9f + 1.3f * high + 1.1f * pulse) * gain
        out.setAppearance(PartFamily.ANTLERS, 0, color.r * antlerGain, color.g * antlerGain, color.b * antlerGain, true)

        // Mouth glow, brighter as the jaw opens.
        toModel(0f, -0.36f, 2.0f, tmp)
        out.setTransform(PartFamily.MOUTH_GLOW, 0, tmp.x, tmp.y, tmp.z, rot, 0.55f * s, 0.07f * s, 1.8f * s)
        val mouthGain = (0.3f + 4f * jawAngle) * gain
        out.setAppearance(PartFamily.MOUTH_GLOW, 0, a.r * mouthGain, a.g * mouthGain, a.b * mouthGain, true)
    }

    companion object {
        const val SCALE = 1.1f
        const val JAW_MAX = 0.6f
        const val JAW_PIVOT_X = 0f
        const val JAW_PIVOT_Y = -0.3f
        const val JAW_PIVOT_Z = -0.6f
        /** Breath leaves the mouth here, in head space. */
        const val MOUTH_X = 0f
        const val MOUTH_Y = -0.36f
        const val MOUTH_Z = 3.6f
        private const val PI_F = 3.1415927f

        /** out = rotation for Euler angles applied in X, then Y, then Z order. */
        fun eulerXYZ(x: Float, y: Float, z: Float, out: Quat): Quat {
            val cx = cos(x / 2); val sx = sin(x / 2)
            val cy = cos(y / 2); val sy = sin(y / 2)
            val cz = cos(z / 2); val sz = sin(z / 2)
            // q = qz · qy · qx (x applied first).
            return out.set(
                sx * cy * cz - cx * sy * sz,
                cx * sy * cz + sx * cy * sz,
                cx * cy * sz - sx * sy * cz,
                cx * cy * cz + sx * sy * sz,
            )
        }
    }
}
