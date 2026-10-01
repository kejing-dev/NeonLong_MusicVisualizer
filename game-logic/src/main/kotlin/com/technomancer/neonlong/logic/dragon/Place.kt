package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Quat
import com.technomancer.neonlong.logic.math.Vec3

/** Shared ways of writing a part's transform. Reuses its own temporaries; not thread-safe. */
class Place {
    private val dir = Vec3()
    private val rot = Quat()
    private val turn = Quat()

    /**
     * A base-anchored mesh (spanning y ∈ [0, 1]) stretched from [from] along [toward] − [from]
     * ([rx], length, [rz] as scale). Also returns the length.
     */
    fun along(out: PoseBuffer, family: PartFamily, n: Int, from: Vec3, toward: Vec3, rx: Float, rz: Float): Float {
        dir.set(toward).sub(from)
        val len = dir.length()
        dir.normalize()
        rot.setFromUpTo(dir)
        out.setTransform(family, n, from.x, from.y, from.z, rot, rx, len, rz)
        return len
    }

    /** Like [along] with a unit direction [d] and length [len]. */
    fun alongDir(out: PoseBuffer, family: PartFamily, n: Int, from: Vec3, d: Vec3, len: Float, rx: Float, rz: Float) {
        rot.setFromUpTo(d)
        out.setTransform(family, n, from.x, from.y, from.z, rot, rx, len, rz)
    }

    /** A ring mesh lying in its XY plane, turned so its axis (+Z) points along [d], uniform [size]. */
    fun ringFacing(out: PoseBuffer, family: PartFamily, n: Int, at: Vec3, d: Vec3, size: Float) {
        rot.setFromUpTo(d)
        // Local +Z → +Y first (−90° about X), then +Y → d.
        turn.set(-0.70710677f, 0f, 0f, 0.70710677f)
        rot.mul(rot, turn)
        out.setTransform(family, n, at.x, at.y, at.z, rot, size, size, size)
    }
}
