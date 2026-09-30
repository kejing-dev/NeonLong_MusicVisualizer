package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat

/**
 * Preallocated output of one simulation step, read by the render Systems.
 *
 * For each [PartFamily], instance n owns 10 transform floats at `n * 10`
 * (position xyz, rotation quaternion xyzw, scale xyz) and 4 appearance floats at `n * 4`
 * (emissive rgb, visibility 0/1). Positions are in model units.
 */
class PoseBuffer {
    private val families = PartFamily.entries.toTypedArray()
    private val transforms = Array(families.size) { FloatArray(families[it].capacity * TRANSFORM_STRIDE) }
    private val appearances = Array(families.size) { FloatArray(families[it].capacity * APPEARANCE_STRIDE) }

    fun transforms(family: PartFamily): FloatArray = transforms[family.ordinal]

    fun appearance(family: PartFamily): FloatArray = appearances[family.ordinal]

    /** Writes instance [n] of [family] from a frame's position, a rotation and a scale. */
    fun setTransform(family: PartFamily, n: Int, frame: Basis, rot: Quat, sx: Float, sy: Float, sz: Float) {
        val t = transforms[family.ordinal]
        val i = n * TRANSFORM_STRIDE
        t[i] = frame.p.x; t[i + 1] = frame.p.y; t[i + 2] = frame.p.z
        t[i + 3] = rot.x; t[i + 4] = rot.y; t[i + 5] = rot.z; t[i + 6] = rot.w
        t[i + 7] = sx; t[i + 8] = sy; t[i + 9] = sz
    }

    fun setAppearance(family: PartFamily, n: Int, emissive: Color3, visible: Boolean) {
        val a = appearances[family.ordinal]
        val i = n * APPEARANCE_STRIDE
        a[i] = emissive.r; a[i + 1] = emissive.g; a[i + 2] = emissive.b
        a[i + 3] = if (visible) 1f else 0f
    }

    companion object {
        const val TRANSFORM_STRIDE = 10
        const val APPEARANCE_STRIDE = 4
    }
}
