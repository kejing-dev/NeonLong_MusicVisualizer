package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.math.Easing

/** Where the body segments sit along the spine and how big they are. */
object BodyLayout {
    /** Body radius at spine position [u]: full near the neck, tapering to 0.29 at the tail. */
    fun radiusAt(u: Float): Float =
        1.45f * (1f - 0.8f * Easing.smooth(0.5f, 1f, u)) * (0.78f + 0.22f * Easing.smooth(0f, 0.14f, u))

    /** Spine position of segment [i] (0 at the head). */
    fun segmentU(i: Int): Float = 0.045f + (i.toFloat() / (DragonConfig.SEGMENTS - 1)) * 0.93f

    /** Segment depth along the spine for body radius [r]. */
    fun segmentDepth(r: Float): Float = 1.3f * (0.55f + 0.45f * r / 1.45f)
}
