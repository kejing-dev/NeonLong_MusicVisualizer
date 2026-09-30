package com.technomancer.neonlong.input

import android.util.Log
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.tracking.DataProvider
import com.pico.spatial.tracking.hand.HandJoint
import com.pico.spatial.tracking.hand.HandPose
import com.pico.spatial.tracking.hand.HandTrackingProvider
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.logic.input.HandFrame

/**
 * Reads hand joints from the Tracking Pack into [HandFrame]s. The SDK only provides hand data
 * in Full Space, so in the volume this stays idle and says so once in the log.
 */
class HandTrackingAdapter {
    private val provider = HandTrackingProvider()
    private var loggedState: DataProvider.SupportState? = null

    val available: Boolean
        get() = provider.supportState == DataProvider.SupportState.SUPPORTED &&
            provider.state == DataProvider.State.STARTED

    fun start() {
        val result = try { provider.start().toString() } catch (e: Throwable) { "failed: ${e.message}" }
        Log.i(LOG_TAG, "[input] hand tracking start: $result, support ${provider.supportState}")
    }

    fun stop() {
        try { provider.stop() } catch (_: Throwable) {}
    }

    /** Fills [left] and [right]; both are untracked when no data is available. */
    fun read(left: HandFrame, right: HandFrame) {
        val support = provider.supportState
        if (support != loggedState) {
            loggedState = support
            Log.i(LOG_TAG, "[input] hand tracking support: $support")
        }
        val data = if (available) provider.latestData else null
        fill(data?.left, left, isLeft = true)
        fill(data?.right, right, isLeft = false)
    }

    private fun fill(pose: HandPose?, out: HandFrame, isLeft: Boolean) {
        if (pose == null) {
            out.tracked = false
            return
        }
        val palm = pose.get(HandJoint.Index.PALM)?.position
        val wrist = pose.get(HandJoint.Index.WRIST)?.position
        val index = pose.get(HandJoint.Index.INDEX_METACARPAL)?.position
        val little = pose.get(HandJoint.Index.LITTLE_METACARPAL)?.position
        if (palm == null || wrist == null || index == null || little == null) {
            out.tracked = false
            return
        }
        out.tracked = true
        out.palm.set(palm.x, palm.y, palm.z)
        // Palm normal from the plane of wrist and two metacarpals; the sign flips per hand.
        val ax = index.x - wrist.x; val ay = index.y - wrist.y; val az = index.z - wrist.z
        val bx = little.x - wrist.x; val by = little.y - wrist.y; val bz = little.z - wrist.z
        out.palmNormal.set(ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx).normalize()
        if (isLeft) out.palmNormal.scale(-1f)
        out.open = fingersExtended(pose, palm)
    }

    /** Open hand: the four fingertips are on average more than 7 cm from the palm. */
    private fun fingersExtended(pose: HandPose, palm: Vector3): Boolean {
        var sum = 0f
        var n = 0
        for (tip in TIPS) {
            val p = pose.get(tip)?.position ?: continue
            val dx = p.x - palm.x; val dy = p.y - palm.y; val dz = p.z - palm.z
            sum += kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
            n++
        }
        return n > 0 && sum / n > 0.07f
    }

    private companion object {
        val TIPS = arrayOf(
            HandJoint.Index.INDEX_TIP,
            HandJoint.Index.MIDDLE_TIP,
            HandJoint.Index.RING_TIP,
            HandJoint.Index.LITTLE_TIP,
        )
    }
}
