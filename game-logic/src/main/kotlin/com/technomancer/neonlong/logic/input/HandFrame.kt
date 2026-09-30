package com.technomancer.neonlong.logic.input

import com.technomancer.neonlong.logic.math.Vec3

/** One hand's state for a frame, in meters, in the same space as the gesture target. */
class HandFrame {
    var tracked = false
    /** Palm center. */
    val palm = Vec3()
    /** Unit vector out of the palm's front face. */
    val palmNormal = Vec3(0f, 0f, -1f)
    /** True when the fingers are extended (not a fist or pinch). */
    var open = false

    fun set(o: HandFrame): HandFrame {
        tracked = o.tracked
        palm.set(o.palm)
        palmNormal.set(o.palmNormal)
        open = o.open
        return this
    }
}
