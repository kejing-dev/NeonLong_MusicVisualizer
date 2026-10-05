package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * The flaming pearl the dragon chases: it flies a fixed distance ahead of the head on the
 * loop, pulsing with the volume. A glowing core, a spinning glass shell, three rings for bass, mids and highs, and
 * 14 flame ribbons. After a Call it parks near the front of the volume for a few seconds.
 */
class Pearl {
    val center = Vec3()
    private val rot = Quat()
    private val color = Color3()
    private val to = Color3()
    private val white = Color3(1f, 1f, 1f)
    private var shellAngle = 0f
    private var ringTime = 0f

    private val target = Vec3()
    private val base = Vec3()
    private val tan = Vec3()
    private var placed = false

    /**
     * Places the pearl a fixed distance ahead of the head along the flight loop, so it leads
     * the dragon down the same path. It also takes on most of the head's sway away from the
     * loop, bobs gently, and eases toward that spot instead of jumping. [pull] and [frontZ]
     * apply the same loop pull as the spine; while [parked] it eases toward a spot near the front.
     */
    fun place(sHead: Float, pull: Float, frontZ: Float, parked: Boolean, parkBlend: Float, head: Vec3, t: Float, dt: Float) {
        // Walk LEAD units of path length ahead of the head (the loop's speed per radian varies).
        var s = sHead
        val stepLen = LEAD / LEAD_STEPS
        for (i in 0 until LEAD_STEPS) {
            FlightPath.tangent(s, tan)
            s += stepLen / tan.length().coerceAtLeast(1f)
        }
        FlightPath.basePt(s, target)
        target.z = target.z * (1f - pull) + pull * frontZ
        // The head's offset from its own spot on the loop (body waves, roll).
        FlightPath.basePt(sHead, base)
        base.z = base.z * (1f - pull) + pull * frontZ
        target.x += (head.x - base.x) * FOLLOW
        target.y += (head.y - base.y) * FOLLOW + RISE + BOB * sin(1.3f * t)
        target.z += (head.z - base.z) * FOLLOW
        if (parked || parkBlend > 0f) {
            val k = parkBlend
            target.x += (0f - target.x) * k
            target.y += (PARK_Y - target.y) * k
            target.z += (frontZ - target.z) * k
        }
        if (!placed) {
            center.set(target)
            placed = true
        } else {
            val k = 1f - exp(-EASE * dt)
            center.x += (target.x - center.x) * k
            center.y += (target.y - center.y) * k
            center.z += (target.z - center.z) * k
        }
    }

    fun write(
        out: PoseBuffer, fibers: Fibers, palette: Palette, dt: Float, t: Float,
        bass: Float, mid: Float, high: Float, vol: Float, pulse: Float, reducedMotion: Boolean, gain: Float,
    ) {
        val ps = 0.9f + 0.6f * vol + 0.3f * pulse
        val a = palette.accent
        rot.identity()
        out.setTransform(PartFamily.PEARL_CORE, 0, center.x, center.y, center.z, rot, ps, ps, ps)
        color.lerp(a, white, 0.35f)
        val coreGain = (1.6f + 2.5f * vol) * gain
        out.setAppearance(PartFamily.PEARL_CORE, 0, color.r * coreGain, color.g * coreGain, color.b * coreGain, true)

        shellAngle += 0.4f * dt
        rot.set(0f, sin(shellAngle / 2), 0f, cos(shellAngle / 2))
        out.setTransform(PartFamily.PEARL_SHELL, 0, center.x, center.y, center.z, rot, ps, ps, ps)
        out.setAppearance(PartFamily.PEARL_SHELL, 0, 0f, 0f, 0f, true)

        ringTime += dt * (if (reducedMotion) 0.3f else 1f)
        val tt = ringTime
        for (k in 0..2) {
            HeadRig.eulerXYZ(tt * (0.4f + 0.3f * k) + k, tt * (0.6f - 0.2f * k), 0f, rot)
            val size = (1f + 0.22f * k) * ps
            out.setTransform(PartFamily.PEARL_RING, k, center.x, center.y, center.z, rot, size, size, size)
            val c = when (k) { 0 -> palette.stop0; 1 -> a; else -> palette.stop2 }
            val x = when (k) { 0 -> bass; 1 -> mid; else -> high }
            val g = (1.2f + 2.5f * x) * gain
            out.setAppearance(PartFamily.PEARL_RING, k, c.r * g, c.g * g, c.b * g, true)
        }

        // Flame ribbons rising from a ring around the pearl.
        val len0 = 0.6f + 1.2f * vol
        for (i in 0 until FLAMES) {
            val fib = fibers.pearlFlames[i]
            val an = (2.0 * PI * i / FLAMES).toFloat()
            val bx = center.x + 0.45f * ps * cos(an)
            val by = center.y - 0.1f
            val bz = center.z + 0.45f * ps * sin(an)
            val len = (1.2f + 0.35f * (i % 3)) * len0
            palette.palAt(i / 13f, to)
            for (k in 0 until fib.points) {
                val aa = k / (fib.points - 1f)
                fib.xyz[k * 3] = bx + 0.5f * aa * cos(an) + 0.2f * aa * sin(6f * t + i + 5f * aa)
                fib.xyz[k * 3 + 1] = by + aa * len
                fib.xyz[k * 3 + 2] = bz + 0.5f * aa * sin(an) + 0.2f * aa * cos(5f * t + i + 4f * aa)
                fib.radius[k] = 0.09f + (0.003f - 0.09f) * aa
                fib.rgb[k * 3] = (a.r * 1.8f + (to.r * 1.2f - a.r * 1.8f) * aa) * gain
                fib.rgb[k * 3 + 1] = (a.g * 1.8f + (to.g * 1.2f - a.g * 1.8f) * aa) * gain
                fib.rgb[k * 3 + 2] = (a.b * 1.8f + (to.b * 1.2f - a.b * 1.8f) * aa) * gain
            }
        }
    }

    companion object {
        const val FLAMES = 14
        /** Height the pearl parks at after a Call (model units). */
        const val PARK_Y = 2.5f
        /** How far ahead of the head the pearl flies, along the loop (model units). */
        const val LEAD = 9.5f
        private const val LEAD_STEPS = 6
        /** Share of the head's sway the pearl takes on. */
        const val FOLLOW = 0.75f
        /** Height above the loop, and how far it bobs up and down. */
        const val RISE = 0.9f
        const val BOB = 0.25f
        /** How quickly it eases to its spot, per second. */
        const val EASE = 5f
    }
}
