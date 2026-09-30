package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.DragonConfig
import kotlin.math.abs

/** The model-space box every part must stay inside. */
class Bounds(private val c: DragonConfig) {
    fun contains(x: Float, y: Float, z: Float): Boolean =
        abs(x - c.boundsCenterX) <= c.boundsHalfX &&
            abs(y - c.boundsCenterY) <= c.boundsHalfY &&
            abs(z - c.boundsCenterZ) <= c.boundsHalfZ
}
