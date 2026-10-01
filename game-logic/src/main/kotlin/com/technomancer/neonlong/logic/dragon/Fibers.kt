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

    companion object {
        /** Sides around the core tube. */
        const val CORE_SIDES = 12
    }
}
