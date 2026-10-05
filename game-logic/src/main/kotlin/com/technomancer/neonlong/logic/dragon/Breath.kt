package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat
import com.technomancer.neonlong.logic.math.Rng
import com.technomancer.neonlong.logic.math.Vec3
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Light particles blown from the mouth on strong bass and beats. They live in the room's
 * frame, not the dragon's: a particle takes the mouth's position and direction at the moment
 * it's emitted and is on its own after that, so it doesn't follow the head as it turns, or the
 * dragon when the user turns it on its turntable. The renderer draws them outside the dragon.
 *
 * A fixed pool that's reused: nothing is allocated after construction. A particle's slot is
 * freed the moment it has faded out or left the bounds box, so nothing escapes the volume.
 * The update is frame-rate independent (emission accumulates fractionally, drag uses
 * exp(−k·dt)).
 */
class Breath(private val bounds: Bounds, capacity: Int = 700, seed: Int = 77) {
    val capacity = capacity
    private val px = FloatArray(capacity); private val py = FloatArray(capacity); private val pz = FloatArray(capacity)
    private val vx = FloatArray(capacity); private val vy = FloatArray(capacity); private val vz = FloatArray(capacity)
    /** Slow drift each particle keeps after its throw has died down. */
    private val dx = FloatArray(capacity); private val dy = FloatArray(capacity); private val dz = FloatArray(capacity)
    private val life = FloatArray(capacity)
    private var lastMx = 0f; private var lastMy = 0f; private var lastMz = 0f
    private var hasLastMouth = false
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

    /**
     * Emits and moves the particles. [yaw] is the dragon's turntable angle in radians: the
     * mouth's position and direction are turned by it once, at emission, into the room's
     * frame. From then on a particle knows nothing about the dragon.
     */
    fun update(dt: Float, head: HeadRig, bass: Float, beatStrength: Float, yaw: Float = 0f) {
        // Where the mouth is in the room, and how fast it's moving there.
        head.toModel(HeadRig.MOUTH_X, HeadRig.MOUTH_Y, HeadRig.MOUTH_Z, mouth)
        val c = cos(yaw)
        val s = sin(yaw)
        val cx = bounds.centerX
        val cz = bounds.centerZ
        val mx = cx + (mouth.x - cx) * c + (mouth.z - cz) * s
        val my = mouth.y
        val mz = cz - (mouth.x - cx) * s + (mouth.z - cz) * c
        var headSpeed = 0f
        if (hasLastMouth && dt > 1e-4f) {
            val ex = mx - lastMx; val ey = my - lastMy; val ez = mz - lastMz
            headSpeed = min(sqrt(ex * ex + ey * ey + ez * ez) / dt, MAX_HEAD_SPEED)
        }
        lastMx = mx; lastMy = my; lastMz = mz
        hasLastMouth = true

        // Emit: continuous on bass above 0.35, plus a burst on each beat.
        carry += maxOf(0f, bass - 0.35f) * EMIT_RATE * dt
        if (beatStrength > 0f) carry += BEAT_BURST
        val toEmit = floor(carry).toInt()
        carry -= toEmit
        if (toEmit > 0) {
            val f = head.frame
            for (k in 0 until toEmit) {
                val i = next
                next = (next + 1) % capacity
                // Thrown faster than the head is flying, so the head never catches its own breath.
                val fwd = rng.range(SPEED_MIN, SPEED_MAX) + headSpeed
                val side = rng.range(-SPREAD, SPREAD)
                val up = rng.range(-SPREAD, SPREAD) - 0.8f
                val wx = f.f.x * fwd + f.s.x * side + f.u.x * up
                val wy = f.f.y * fwd + f.s.y * side + f.u.y * up
                val wz = f.f.z * fwd + f.s.z * side + f.u.z * up
                px[i] = mx; py[i] = my; pz[i] = mz
                vx[i] = wx * c + wz * s
                vy[i] = wy
                vz[i] = -wx * s + wz * c
                dx[i] = rng.range(-DRIFT, DRIFT)
                dy[i] = rng.range(0f, DRIFT)
                dz[i] = rng.range(-DRIFT, DRIFT)
                life[i] = 1f
                born[i] = spawned++
            }
        }
        val drag = exp(-DRAG * dt)
        for (i in 0 until capacity) {
            if (life[i] <= 0f) continue
            life[i] -= dt / LIFE_S
            vx[i] *= drag; vy[i] = vy[i] * drag + 1.5f * dt; vz[i] *= drag
            px[i] += (vx[i] + dx[i]) * dt; py[i] += (vy[i] + dy[i]) * dt; pz[i] += (vz[i] + dz[i]) * dt
            // Faded out or out of the volume: the slot is free for the next particle.
            if (life[i] <= 0f || !bounds.contains(px[i], py[i], pz[i])) life[i] = 0f
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
                // Drawn as soft round glows: big and bright when new, shrinking as they fade.
                val size = SIZE_MIN + (SIZE_MAX - SIZE_MIN) * l
                out.setTransform(PartFamily.BREATH, n, px[i], py[i], pz[i], rot, size, size, size)
                palette.palAt(1f - l, color)
                color.lerp(color, palette.accent, l * l)
                val g = GLOW * l * gain
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
        hasLastMouth = false
    }

    companion object {
        /** Particles per second per unit of bass above 0.35, and extra particles on each beat. */
        const val EMIT_RATE = 520f
        const val BEAT_BURST = 60f
        /**
         * Speed leaving the mouth, on top of the head's own speed (model units per second),
         * and how fast it's lost. The throw alone carries a particle 6 to 10 units.
         */
        const val SPEED_MIN = 12f
        const val SPEED_MAX = 20f
        const val DRAG = 2.0f
        /** Sideways and vertical spread of the throw (model units per second). */
        const val SPREAD = 2.2f
        /** The head's speed is capped at this when added to the throw. */
        const val MAX_HEAD_SPEED = 16f
        /** Slow sideways and upward drift on top of the throw (model units per second). */
        const val DRIFT = 0.5f
        /** How long a particle lives, in seconds. */
        const val LIFE_S = 1.1f
        /** Width of a particle's glow at the end and the start of its life (model units). */
        const val SIZE_MIN = 0.18f
        const val SIZE_MAX = 0.6f
        /** Brightness of a new particle. Kept low because neighboring glows add up. */
        const val GLOW = 0.9f
    }
}
