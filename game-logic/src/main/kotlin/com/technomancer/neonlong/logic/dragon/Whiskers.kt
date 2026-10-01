package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.cos
import kotlin.math.sin

/** Two whiskers of 30 glowing dots each, curving out from the snout and swaying with the mids. */
class Whiskers {
    private val p = Vec3()
    private val color = Color3()

    /** [stride] 2 hides every other dot (small volumes). */
    fun write(out: PoseBuffer, head: HeadRig, palette: Palette, t: Float, mid: Float, stride: Int, gain: Float) {
        val sway = 0.35f + 1.6f * mid
        for (q in 0..1) {
            val sg = if (q == 1) 1f else -1f
            for (k in 0 until DOTS) {
                val n = q * DOTS + k
                val a = k / 29f
                val x = sg * (0.45f + 2.6f * a - 0.4f * a * a)
                val y = 0.1f - 0.8f * a + sin(3.1f * t + 5f * a + q) * a * 0.6f * sway
                val z = 3.75f + 0.8f * a - 4.6f * a * a + cos(2.3f * t + 4f * a) * a * 0.35f * sway
                head.toModel(x, y, z, p)
                val size = 0.08f * (1f - 0.5f * a) * HeadRig.SCALE
                out.setTransform(PartFamily.WHISKER_DOT, n, p.x, p.y, p.z, head.rot, size, size, size)
                palette.palAt(a, color)
                color.lerp(color, palette.accent, 0.4f)
                val g = (1.4f + 2.5f * mid * (1f - a)) * gain
                out.setAppearance(PartFamily.WHISKER_DOT, n, color.r * g, color.g * g, color.b * g, k % stride == 0)
            }
        }
    }

    companion object {
        const val DOTS = 30
    }
}
