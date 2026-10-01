package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat
import com.technomancer.neonlong.logic.math.Rng
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.exp
import kotlin.math.floor

/**
 * Light particles blown from the mouth on strong bass and beats. A fixed pool; the update is
 * frame-rate independent (emission accumulates fractionally, drag uses exp(−k·dt)), and
 * particles that leave the bounds box are retired so nothing escapes the volume.
 */
class Breath(private val bounds: Bounds, capacity: Int = 700, seed: Int = 77) {
    val capacity = capacity
    private val px = FloatArray(capacity); private val py = FloatArray(capacity); private val pz = FloatArray(capacity)
    private val vx = FloatArray(capacity); private val vy = FloatArray(capacity); private val vz = FloatArray(capacity)
    private val life = FloatArray(capacity)
    /** Spawn order, so the renderer can draw the newest particles. */
    private val born = LongArray(capacity)
    private var next = 0
    private var spawned = 0L
    private var carry = 0f
    private val rng = Rng(seed)
    private val mouth = Vec3()
    private val color = Color3()
    private val rot = Quat()

    val liveCount: Int get() = life.count { it > 0f }

    /** Mean position of live particles (for tests); writes into [out]. */
    fun meanPosition(out: Vec3): Vec3 {
        var n = 0
        out.set(0f, 0f, 0f)
        for (i in 0 until capacity) if (life[i] > 0f) {
            out.x += px[i]; out.y += py[i]; out.z += pz[i]; n++
        }
        if (n > 0) out.scale(1f / n)
        return out
    }

    fun update(dt: Float, head: HeadRig, bass: Float, beatStrength: Float) {
        // Emit: continuous on bass above 0.35, plus a burst on each beat.
        carry += maxOf(0f, bass - 0.35f) * 500f * dt
        if (beatStrength > 0f) carry += 45f
        val toEmit = floor(carry).toInt()
        carry -= toEmit
        if (toEmit > 0) {
            head.toModel(HeadRig.MOUTH_X, HeadRig.MOUTH_Y, HeadRig.MOUTH_Z, mouth)
            val f = head.frame
            for (k in 0 until toEmit) {
                val i = next
                next = (next + 1) % capacity
                val fwd = rng.range(9f, 16f)
                val side = rng.range(-1.75f, 1.75f)
                val up = rng.range(-1.75f, 1.75f) - 0.8f
                px[i] = mouth.x; py[i] = mouth.y; pz[i] = mouth.z
                vx[i] = f.f.x * fwd + f.s.x * side + f.u.x * up
                vy[i] = f.f.y * fwd + f.s.y * side + f.u.y * up
                vz[i] = f.f.z * fwd + f.s.z * side + f.u.z * up
                life[i] = 1f
                born[i] = spawned++
            }
        }
        val drag = exp(-1.827f * dt)
        for (i in 0 until capacity) {
            if (life[i] <= 0f) continue
            life[i] -= 1.1f * dt
            vx[i] *= drag; vy[i] = vy[i] * drag + 1.5f * dt; vz[i] *= drag
            px[i] += vx[i] * dt; py[i] += vy[i] * dt; pz[i] += vz[i] * dt
            if (!bounds.contains(px[i], py[i], pz[i])) life[i] = 0f
        }
    }

    /**
     * Writes the newest live particles into the BREATH family (up to its capacity); the rest of
     * the drawn slots are hidden.
     */
    fun write(out: PoseBuffer, palette: Palette, gain: Float) {
        val slots = PartFamily.BREATH.capacity
        rot.identity()
        var n = 0
        // Walk backwards from the most recently spawned slot.
        var i = (next - 1 + capacity) % capacity
        var seen = 0
        while (seen < capacity && n < slots) {
            val l = life[i]
            if (l > 0f) {
                val size = 0.03f + 0.09f * l
                out.setTransform(PartFamily.BREATH, n, px[i], py[i], pz[i], rot, size, size, size)
                palette.palAt(1f - l, color)
                color.lerp(color, palette.accent, l * l)
                val g = 1.4f * l * gain
                out.setAppearance(PartFamily.BREATH, n, color.r * g, color.g * g, color.b * g, true)
                n++
            }
            i = (i - 1 + capacity) % capacity
            seen++
        }
        while (n < slots) {
            out.setAppearance(PartFamily.BREATH, n, 0f, 0f, 0f, false)
            n++
        }
    }

    /** Every live particle's position, for bounds checks: calls [visit] with x, y, z. */
    inline fun forEachLive(visit: (Float, Float, Float) -> Unit) {
        for (i in 0 until capacity) if (isLive(i)) visit(x(i), y(i), z(i))
    }

    fun isLive(i: Int) = life[i] > 0f
    fun x(i: Int) = px[i]
    fun y(i: Int) = py[i]
    fun z(i: Int) = pz[i]

    fun clear() {
        life.fill(0f)
        carry = 0f
    }
}
