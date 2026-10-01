package com.technomancer.neonlong.logic.dragon

/**
 * A glowing tube drawn through a polyline: per point a position, a radius and an emissive
 * color. Preallocated for [points] points.
 */
class Fiber(val points: Int) {
    val xyz = FloatArray(points * 3)
    val radius = FloatArray(points)
    val rgb = FloatArray(points * 3)
    var visible = true
}

/** Every fiber family's buffers. */
class Fibers(spinePoints: Int) {
    /** Energy core: a tube along all spine samples. */
    val core = Fiber(spinePoints)
    /** Flame tufts at the elbows: 5 per leg, 8 points each. */
    val tufts = Array(20) { Fiber(8) }
    /** Tail flame fan: 18 ribbons of 13 points. */
    val tail = Array(18) { Fiber(13) }
    /** Pearl flames: 14 of 9 points. */
    val pearlFlames = Array(14) { Fiber(9) }
    /** Side cables along the body: 2 of 110 points. */
    val cables = Array(2) { Fiber(110) }

    companion object {
        /** Sides around the core tube. */
        const val CORE_SIDES = 12
    }
}
