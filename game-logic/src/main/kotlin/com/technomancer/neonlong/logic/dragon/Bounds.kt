package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import kotlin.math.abs

/** The model-space box every part must stay inside. */
class Bounds(private val c: DragonConfig) {
    /** The box's center on the ground plane; the turntable turns the dragon about this point. */
    val centerX: Float get() = c.boundsCenterX
    val centerZ: Float get() = c.boundsCenterZ

    fun contains(x: Float, y: Float, z: Float): Boolean =
        abs(x - c.boundsCenterX) <= c.boundsHalfX &&
            abs(y - c.boundsCenterY) <= c.boundsHalfY &&
            abs(z - c.boundsCenterZ) <= c.boundsHalfZ
}
