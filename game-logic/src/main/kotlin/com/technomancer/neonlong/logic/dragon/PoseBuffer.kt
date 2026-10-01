package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat

/**
 * Preallocated output of one simulation step, read by the render Systems.
 *
 * For each [PartFamily], instance n owns 10 transform floats at `n * 10`
 * (position xyz, rotation quaternion xyzw, scale xyz) and 4 appearance floats at `n * 4`
 * (emissive rgb, visibility 0/1). Positions are in model units. Tubes live in [fibers].
 */
class PoseBuffer(spinePoints: Int = 241) {
    private val families = PartFamily.entries.toTypedArray()
    private val transforms = Array(families.size) { FloatArray(families[it].capacity * TRANSFORM_STRIDE) }
    private val appearances = Array(families.size) { FloatArray(families[it].capacity * APPEARANCE_STRIDE) }

    val fibers = Fibers(spinePoints)

    fun transforms(family: PartFamily): FloatArray = transforms[family.ordinal]

    fun appearance(family: PartFamily): FloatArray = appearances[family.ordinal]

    /** Writes instance [n] of [family] at a frame's position, with a rotation and a scale. */
    fun setTransform(family: PartFamily, n: Int, frame: Basis, rot: Quat, sx: Float, sy: Float, sz: Float) =
        setTransform(family, n, frame.p.x, frame.p.y, frame.p.z, rot, sx, sy, sz)

    fun setTransform(
        family: PartFamily, n: Int,
        px: Float, py: Float, pz: Float,
        rot: Quat, sx: Float, sy: Float, sz: Float,
    ) {
        val t = transforms[family.ordinal]
        val i = n * TRANSFORM_STRIDE
        t[i] = px; t[i + 1] = py; t[i + 2] = pz
        t[i + 3] = rot.x; t[i + 4] = rot.y; t[i + 5] = rot.z; t[i + 6] = rot.w
        t[i + 7] = sx; t[i + 8] = sy; t[i + 9] = sz
    }

    fun setAppearance(family: PartFamily, n: Int, emissive: Color3, visible: Boolean) =
        setAppearance(family, n, emissive.r, emissive.g, emissive.b, visible)

    fun setAppearance(family: PartFamily, n: Int, r: Float, g: Float, b: Float, visible: Boolean) {
        val a = appearances[family.ordinal]
        val i = n * APPEARANCE_STRIDE
        a[i] = r; a[i + 1] = g; a[i + 2] = b
        a[i + 3] = if (visible) 1f else 0f
    }

    companion object {
        const val TRANSFORM_STRIDE = 10
        const val APPEARANCE_STRIDE = 4
    }
}
