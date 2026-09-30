package com.technomancer.neonlong.app

import android.util.Log
import com.pico.spatial.core.math.Quat

/**
 * Builds SDK quaternions from (x, y, z, w). The SDK's four-float constructor order isn't
 * documented, so the first call checks it once: an identity built as (0, 0, 0, 1) must
 * report w = 1.
 */
object SdkMath {
    private val wFirst: Boolean by lazy {
        val probe = Quat(0f, 0f, 0f, 1f)
        val result = probe.w != 1f
        Log.i(LOG_TAG, "[boot] SDK Quat constructor order: ${if (result) "w,x,y,z" else "x,y,z,w"}")
        result
    }

    fun quat(x: Float, y: Float, z: Float, w: Float): Quat =
        if (wFirst) Quat(w, x, y, z) else Quat(x, y, z, w)
}
