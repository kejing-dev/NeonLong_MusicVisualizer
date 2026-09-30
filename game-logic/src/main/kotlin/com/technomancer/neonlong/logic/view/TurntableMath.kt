package com.technomancer.neonlong.logic.view

import kotlin.math.PI
import kotlin.math.atan2

/** Converts hand movement around the turntable into dragon yaw. */
object TurntableMath {
    private const val TWO_PI = (2.0 * PI).toFloat()

    /**
     * Yaw change (radians, counter-clockwise seen from above) for a hand moving from
     * (x0, z0) to (x1, z1), both relative to the turntable center, times [gain].
     * 1° of hand arc gives [gain]° of yaw.
     */
    fun yawFromArc(x0: Float, z0: Float, x1: Float, z1: Float, gain: Float): Float {
        // Angle measured from +X toward −Z so positive means counter-clockwise from above (Y up).
        val a0 = atan2(-z0, x0)
        val a1 = atan2(-z1, x1)
        return wrap(a1 - a0) * gain
    }

    /**
     * Yaw change for a hand sliding [lateral] meters along the front of a ring with radius
     * [radius] (rightward is positive, turning the front of the ring to the right), times [gain].
     */
    fun yawFromSlide(lateral: Float, radius: Float, gain: Float): Float =
        if (radius <= 1e-6f) 0f else (lateral / radius) * gain

    /** Wraps an angle into (−π, π]. */
    fun wrap(a: Float): Float {
        var x = a % TWO_PI
        if (x > PI) x -= TWO_PI
        if (x <= -PI) x += TWO_PI
        return x
    }

    /** Number of whole [stepDeg] steps crossed going from [fromRad] to [toRad] (for haptic ticks). */
    fun ticksCrossed(fromRad: Float, toRad: Float, stepDeg: Float): Int {
        val step = stepDeg * PI.toFloat() / 180f
        val a = kotlin.math.floor(fromRad / step).toInt()
        val b = kotlin.math.floor(toRad / step).toInt()
        return kotlin.math.abs(b - a)
    }
}
