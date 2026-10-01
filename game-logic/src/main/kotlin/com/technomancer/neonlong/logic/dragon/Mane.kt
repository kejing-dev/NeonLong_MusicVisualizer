package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Two rows of 12 blades fanned across the back of the head. Blade k reads band 44 + k and
 * lifts and grows with it; every blade stays at least 30° off the neck's backward axis.
 */
class Mane {
    private val base = Vec3()
    private val dir = Vec3()
    private val color = Color3()
    private val local = Quat()
    private val rot = Quat()

    /** Lift (radians) of a blade in [row] for drive [mv]. */
    fun lift(row: Int, mv: Float): Float = (if (row == 1) LIFT_ROW1 else LIFT_ROW0) + 0.18f * mv

    /** Unit direction of a blade in head space at fan angle [phi] and lift [l]. */
    fun headDir(phi: Float, l: Float, out: Vec3): Vec3 =
        out.set(sin(phi) * sin(l), cos(phi) * sin(l), -cos(l))

    fun write(out: PoseBuffer, head: HeadRig, palette: Palette, bands: FloatArray, vol: Float, pulse: Float, gain: Float) {
        val s = HeadRig.SCALE
        for (k in 0 until BLADES) {
            val row = k % 2
            val c = k / 2
            val phi = -1.7f + 3.4f * c / 11f
            val v = bands[44 + k]
            val mv = 0.5f * v + 0.35f * vol + 0.25f * pulse
            val l = lift(row, mv)
            head.toModel(0.95f * sin(phi), 0.45f + 0.7f * cos(phi), -1.25f - 0.5f * row, base)
            headDir(phi, l, dir)
            local.setFromUpTo(dir)
            rot.mul(head.rot, local)
            val len = (1.1f + 0.5f * row) + 1.7f * mv
            out.setTransform(PartFamily.MANE_BLADE, k, base.x, base.y, base.z, rot, 0.13f * s, len * s, 0.05f * s)
            palette.palAt(0.5f + c / 24f, color)
            val g = (0.8f + 1.9f * mv) * gain
            out.setAppearance(PartFamily.MANE_BLADE, k, color.r * g, color.g * g, color.b * g, true)
        }
    }

    companion object {
        const val BLADES = 24
        private const val LIFT_ROW0 = 0.7330383f // 42°
        private const val LIFT_ROW1 = 0.5934119f // 34°
    }
}
