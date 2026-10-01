package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Four detached legs that float beside the body and paddle as it flies: armored limbs,
 * chrome joint balls, neon joint rings, palms, toes, talons, and flame tufts at the elbows.
 */
class Legs {
    private val f = Basis()
    private val a = Vec3(); private val e = Vec3(); private val ft = Vec3()
    private val zb = Vec3(); private val d = Vec3(); private val tmp = Vec3(); private val tmp2 = Vec3()
    private val rot = Quat()
    private val color = Color3()
    private val talonColor = Color3()
    private val from = Color3()

    fun write(
        out: PoseBuffer, place: Place, spine: Spine, palette: Palette, waves: Waves, t: Float,
        bass: Float, high: Float, pulse: Float, showSmall: Boolean, gain: Float,
    ) {
        for (leg in 0 until LEGS) {
            val u = LEG_U[leg]
            val s = LEG_SIDE[leg]
            val ph = 2.4f * t + LEG_PHASE[leg]
            spine.frameAt(u, f)
            val r = BodyLayout.radiusAt(u)
            // Shoulder, elbow, foot.
            a.set(f.p).addScaled(f.s, s * (1.3f * r + 0.55f)).addScaled(f.u, -0.5f * r)
            e.set(a).addScaled(f.s, 1.35f * s).addScaled(f.u, -0.6f).addScaled(f.f, 0.7f * sin(ph))
            ft.set(e).addScaled(f.s, 0.35f * s).addScaled(f.u, -1.3f).addScaled(f.f, 0.55f + 0.5f * cos(ph))

            val limbGain = (0.3f + 1.2f * bass + waves.waveAt(u)) * gain
            palette.palAt(u, color)
            place.along(out, PartFamily.LIMB, leg * 2, a, e, 0.3f, 0.3f)
            place.along(out, PartFamily.LIMB, leg * 2 + 1, e, ft, 0.21f, 0.21f)
            for (k in 0..1) out.setAppearance(PartFamily.LIMB, leg * 2 + k, color.r * limbGain, color.g * limbGain, color.b * limbGain, true)

            rot.identity()
            out.setTransform(PartFamily.JOINT_BALL, leg * 2, e.x, e.y, e.z, rot, 0.22f, 0.22f, 0.22f)
            out.setTransform(PartFamily.JOINT_BALL, leg * 2 + 1, a.x, a.y, a.z, rot, 0.26f, 0.26f, 0.26f)
            for (k in 0..1) out.setAppearance(PartFamily.JOINT_BALL, leg * 2 + k, 0f, 0f, 0f, true)

            tmp.set(ft).sub(e).normalize()
            place.ringFacing(out, PartFamily.JOINT_RING, leg * 2, e, tmp, 0.3f)
            tmp.set(e).sub(a).normalize()
            place.ringFacing(out, PartFamily.JOINT_RING, leg * 2 + 1, a, tmp, 0.36f)
            val ringGain = (0.6f + 1.5f * pulse) * gain
            val ac = palette.accent
            for (k in 0..1) out.setAppearance(PartFamily.JOINT_RING, leg * 2 + k, ac.r * ringGain, ac.g * ringGain, ac.b * ringGain, showSmall)

            rot.setFromColumns(f.s, f.u, f.f)
            out.setTransform(PartFamily.PALM, leg, ft.x, ft.y, ft.z, rot, 0.26f, 0.16f, 0.3f)
            out.setAppearance(PartFamily.PALM, leg, color.r * limbGain, color.g * limbGain, color.b * limbGain, true)

            // Toes and talons.
            zb.set(f.f).scale(0.9f).addScaled(f.u, -0.35f).addScaled(f.s, 0.1f * s).normalize()
            talonColor.set(1.2f, 1.2f, 1.3f).lerp(talonColor, ac, 0.25f)
            for (toe in 0..3) {
                if (toe == 3) {
                    d.set(f.f).scale(-0.6f).addScaled(f.s, 0.7f * s).addScaled(f.u, -0.3f).normalize()
                } else {
                    d.set(zb).addScaled(f.s, SPREADS[toe]).addScaled(f.u, -0.1f).normalize()
                }
                val n = leg * 4 + toe
                tmp.set(ft).addScaled(d, 0.12f)
                place.alongDir(out, PartFamily.TOE, n, tmp, d, 0.42f, 0.075f, 0.075f)
                out.setAppearance(PartFamily.TOE, n, color.r * limbGain, color.g * limbGain, color.b * limbGain, true)
                tmp.set(ft).addScaled(d, 0.52f)
                tmp2.set(d).scale(0.55f).addScaled(f.u, -0.8f).normalize()
                place.alongDir(out, PartFamily.TALON, n, tmp, tmp2, 0.34f, 0.055f, 0.055f)
                out.setAppearance(PartFamily.TALON, n, talonColor.r * gain, talonColor.g * gain, talonColor.b * gain, true)
            }
        }
    }

    /** Fills the five flame tufts at each elbow (call after [write], which places the elbows). */
    fun writeTufts(fibers: Fibers, spine: Spine, palette: Palette, t: Float, high: Float, gain: Float) {

        for (leg in 0 until LEGS) {
            val u = LEG_U[leg]
            val s = LEG_SIDE[leg]
            val ph = 2.4f * t + LEG_PHASE[leg]
            spine.frameAt(u, f)
            val r = BodyLayout.radiusAt(u)
            a.set(f.p).addScaled(f.s, s * (1.3f * r + 0.55f)).addScaled(f.u, -0.5f * r)
            e.set(a).addScaled(f.s, 1.35f * s).addScaled(f.u, -0.6f).addScaled(f.f, 0.7f * sin(ph))
            palette.palAt(0.3f, from)
            val tuftGain = (0.7f + 1.5f * high) * gain
            for (k in 0 until TUFTS) {
                val fib = fibers.tufts[leg * TUFTS + k]
                d.set(f.f).scale(-0.85f).addScaled(f.u, 0.25f + 0.12f * k).addScaled(f.s, s * (0.2f + 0.1f * k)).normalize()
                val len = (0.9f + 0.12f * k) * (0.8f + 1.2f * high)
                for (i in 0 until fib.points) {
                    val aa = i / (fib.points - 1f)
                    val wob = sin(5f * t + k + 4f * aa) * 0.18f * aa
                    fib.xyz[i * 3] = e.x + d.x * len * aa + f.u.x * wob
                    fib.xyz[i * 3 + 1] = e.y + d.y * len * aa + f.u.y * wob
                    fib.xyz[i * 3 + 2] = e.z + d.z * len * aa + f.u.z * wob
                    fib.radius[i] = 0.07f + (0.004f - 0.07f) * aa
                    val ac = palette.accent
                    fib.rgb[i * 3] = (from.r * 0.5f + (ac.r * 1.5f - from.r * 0.5f) * aa) * tuftGain
                    fib.rgb[i * 3 + 1] = (from.g * 0.5f + (ac.g * 1.5f - from.g * 0.5f) * aa) * tuftGain
                    fib.rgb[i * 3 + 2] = (from.b * 0.5f + (ac.b * 1.5f - from.b * 0.5f) * aa) * tuftGain
                }
            }
        }
    }

    companion object {
        const val LEGS = 4
        const val TUFTS = 5
        val LEG_U = floatArrayOf(0.17f, 0.17f, 0.60f, 0.60f)
        val LEG_SIDE = floatArrayOf(1f, -1f, 1f, -1f)
        val LEG_PHASE = floatArrayOf(0f, PI.toFloat(), PI.toFloat(), 0f)
        val SPREADS = floatArrayOf(-0.55f, -0.18f, 0.18f, 0.55f)
    }
}
