package com.technomancer.neonlong.logic.dragon

import com.technomancer.neonlong.logic.config.ComfortConfig
import com.technomancer.neonlong.logic.config.DragonConfig
import com.technomancer.neonlong.logic.config.FlashConfig
import com.technomancer.neonlong.logic.config.PaletteConfig
import com.technomancer.neonlong.logic.input.InputSnapshot
import com.technomancer.neonlong.logic.math.Basis
import com.technomancer.neonlong.logic.math.Color3
import com.technomancer.neonlong.logic.math.Quat
import com.technomancer.neonlong.logic.math.Vec3
import com.technomancer.neonlong.logic.safety.FlashLimiter
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Advances the dragon one frame and writes every part's pose and glow into a [PoseBuffer].
 * Pure logic: no allocation per tick, no platform calls.
 *
 * Covers the whole dragon: flight along the loop; the body (dorsal blocks, tiers, belly
 * plates, side plates, square frames, belly lights, 81 towers with spires, caps and beacons,
 * energy core, pistons, cables); the head (jaw, eyes, 龍 plate, antlers, whiskers, mane);
 * legs with flame tufts; the tail fan; the pearl; breath; light waves, touch ripples,
 * Call / Send / Flash; palettes; the flash limiter.
 */
class DragonSimulation(
    private val config: DragonConfig = DragonConfig(),
    private val comfort: ComfortConfig = ComfortConfig(),
    flashConfig: FlashConfig = FlashConfig(),
    paletteConfig: PaletteConfig = PaletteConfig(),
) {
    val spine = Spine(config.spineSamples, config.rollAmp)
    val waves = Waves(config)
    val palette = Palette(paletteConfig)
    val flash = FlashLimiter(flashConfig)
    val towers = Towers()
    val bounds = Bounds(config)
    val head = HeadRig()
    val breath = Breath(bounds)
    val pearl = Pearl()
    private val whiskers = Whiskers()
    private val mane = Mane()
    private val legs = Legs()
    private val tailFan = TailFan()
    private val pistons = Pistons()
    private val place = Place()
    private var parkBlend = 0f

    /** Head position on the flight path, in path radians. */
    var sHead = 0f
        private set

    /** Simulation time in seconds; only advances while ticking. */
    var time = 0f
        private set

    /** True between Call and Send. */
    var called = false
        private set

    /** Current loop pull toward the user, easing toward [DragonConfig.callOffset] or 0. */
    var pull = 0f
        private set

    /** Seconds the pearl stays parked near the user's hand after a Call. */
    var pearlParkRemaining = 0f
        private set

    /** Where the loop is pulled toward on a Call. */
    val frontZ: Float = config.callFrontZ

    private val segs = DragonConfig.SEGMENTS
    private val frames = Array(segs) { Basis() }
    private val segU = FloatArray(segs) { BodyLayout.segmentU(it) }
    private val radius = FloatArray(segs)
    private val depth = FloatArray(segs)
    private val towerV = FloatArray(DragonConfig.TOWERS)

    private val frame = Basis()
    private val rot = Quat()
    private val color = Color3()
    private val tint = Color3()
    private val xb = Vec3()
    private val yb = Vec3()
    private var pendingFlash = false
    private var flashMul = 1f

    /** Clears motion state, e.g. when the container is rebuilt. */
    fun reset() {
        sHead = 0f
        time = 0f
        called = false
        pull = 0f
        pearlParkRemaining = 0f
        pendingFlash = false
        parkBlend = 0f
        waves.clear()
        flash.reset()
        breath.clear()
    }

    /** Draw the loop toward the user and park the pearl near the hand. */
    fun call() {
        called = true
        pearlParkRemaining = config.pearlParkS
    }

    /** Return the loop to its place. */
    fun send() {
        called = false
        pearlParkRemaining = 0f
    }

    /** A full-strength beat on request (clap or button), still subject to the flash limiter. */
    fun flash() {
        pendingFlash = true
    }

    /** A touch on segment [segment]; returns false while it's cooling down. */
    fun touchSegment(segment: Int): Boolean = waves.ripple(segment, segU[segment])

    fun selectPalette(i: Int) {
        palette.select(i)
        flash.redGuardActive = palette.nameOf(palette.index) == "Imperial"
    }

    fun tick(dt: Float, input: InputSnapshot, out: PoseBuffer) {
        val step = if (dt < 0f) 0f else if (dt > config.maxStepS) config.maxStepS else dt
        val motion = if (input.reducedMotion) comfort.reducedMotionFactor else 1f
        time += step
        sHead += step * (config.speedBase + config.speedVol * input.vol + config.speedBass * input.bass) * motion

        val targetPull = if (called) config.callOffset else 0f
        pull += (targetPull - pull) * (1f - exp(-config.callBlendRate * step))
        if (pearlParkRemaining > 0f) pearlParkRemaining -= step

        if (pendingFlash) {
            waves.spawnBeat(1.0f)
            flash.request(1.0f)
            pendingFlash = false
        }
        if (input.beatStrength > 0f) {
            waves.spawnBeat(input.beatStrength)
            flash.request(input.beatStrength)
        }
        waves.update(step)
        flash.update(step)
        palette.update(step)
        flashMul = flash.globalMultiplier

        spine.update(sHead, config.bodyLength, time, input.bass, motion, pull, frontZ)

        for (i in 0 until segs) {
            spine.frameAt(segU[i], frames[i])
            radius[i] = BodyLayout.radiusAt(segU[i])
            depth[i] = BodyLayout.segmentDepth(radius[i])
        }
        writeSegments(input, out)
        writeSidePlates(input, out)
        writeBellyLights(input, out)
        writeTowers(input, out)
        writeCore(input, out)
        writeExtremities(step, input, out)
    }

    /** Head, whiskers, mane, legs, tail fan, pearl, breath, pistons and cables. */
    private fun writeExtremities(step: Float, input: InputSnapshot, out: PoseBuffer) {
        val pulse = input.beatPulse * flash.localPulseScale
        val gain = flashMul
        head.place(spine)
        head.updateJaw(input.bass, input.beatPulse)
        head.write(out, palette, input.bass, input.mid, input.high, pulse, gain)
        whiskers.write(out, head, palette, time, input.mid, input.whiskerStride, gain)
        mane.write(out, head, palette, input.bands, input.vol, pulse, gain)
        legs.write(out, place, spine, palette, waves, time, input.bass, input.high, pulse, input.showSmallParts, gain)
        legs.writeTufts(out.fibers, spine, palette, time, input.high, gain)
        tailFan.write(out.fibers, spine, palette, input.bands, time, input.high, gain)

        val parked = pearlParkRemaining > 0f
        parkBlend += ((if (parked) 1f else 0f) - parkBlend) * (1f - exp(-3f * step))
        pearl.place(sHead, pull, frontZ, parked, parkBlend, head.frame.p, time, step)
        pearl.write(out, out.fibers, palette, step, time, input.bass, input.mid, input.high, input.vol, input.beatPulse, input.reducedMotion, gain)

        breath.update(step, head, input.bass, input.beatStrength, input.dragonYaw)
        breath.write(out, palette, gain)

        pistons.write(out, place, frames, radius, depth, input.showSmallParts)
        pistons.writeCables(out.fibers, spine, palette, input.bass, pulse, input.showSmallParts, gain)
    }

    /** Mean of segment [i]'s three bands. */
    private fun segmentEnergy(input: InputSnapshot, i: Int): Float =
        (input.bands[3 * i] + input.bands[3 * i + 1] + input.bands[3 * i + 2]) / 3f

    private fun emit(out: PoseBuffer, family: PartFamily, n: Int, base: Color3, gain: Float, visible: Boolean = true) {
        val g = gain * flashMul
        out.setAppearance(family, n, base.r * g, base.g * g, base.b * g, visible)
    }

    private fun writeSegments(input: InputSnapshot, out: PoseBuffer) {
        val accent = palette.accent
        for (i in 0 until segs) {
            val f = frames[i]
            val r = radius[i]
            val d = depth[i]
            val u = segU[i]
            val e = segmentEnergy(input, i)
            val w = waves.waveAt(u)
            rot.setFromColumns(f.s, f.u, f.f)
            palette.palAt(u, color)

            out.setTransform(PartFamily.SEG_DORSAL, i, f, rot, 1.9f * r, 1.5f * r, d)
            emit(out, PartFamily.SEG_DORSAL, i, color, 0.35f + 1.5f * e + 1.1f * w + waves.flashOf(i))

            out.setTransform(
                PartFamily.SEG_TIER, i,
                f.p.x + 0.9f * r * f.u.x, f.p.y + 0.9f * r * f.u.y, f.p.z + 0.9f * r * f.u.z,
                rot, 1.3f * r, 0.3f * r, 0.85f * d,
            )
            emit(out, PartFamily.SEG_TIER, i, color, 0.45f + 1.8f * e + 1.2f * w)

            out.setTransform(
                PartFamily.SEG_BELLY, i,
                f.p.x - 0.81f * r * f.u.x, f.p.y - 0.81f * r * f.u.y, f.p.z - 0.81f * r * f.u.z,
                rot, 1.3f * r, 0.12f * r, 0.9f * d,
            )
            emit(out, PartFamily.SEG_BELLY, i, accent, 0.2f + 1.2f * w)

            // Square neon frame just ahead of the segment, pulsing with passing waves.
            val fk = 1.08f + 0.06f * w
            val k = fk / 0.7071f
            out.setTransform(
                PartFamily.FRAME_RING, i,
                f.p.x + 0.52f * d * f.f.x, f.p.y + 0.52f * d * f.f.y, f.p.z + 0.52f * d * f.f.z,
                rot, 0.95f * r * k, 0.8f * r * k, 1f,
            )
            tint.lerp(color, accent, min(0.7f, maxOf(0f, 0.6f * w)))
            emit(out, PartFamily.FRAME_RING, i, tint, 0.5f + 2.6f * w + 0.5f * e)
        }
    }

    private fun writeSidePlates(input: InputSnapshot, out: PoseBuffer) {
        for (i in 0 until segs) {
            val f = frames[i]
            val r = radius[i]
            val w = waves.waveAt(segU[i])
            for (j in 0..2) {
                val row = SidePlates.ROWS[j]
                for (sIdx in 0..1) {
                    val s = if (sIdx == 0) -1 else 1
                    val tilt = row[SidePlates.TILT]
                    val ca = cos(tilt)
                    val sa = s * sin(tilt)
                    yb.set(f.u).scale(ca).addScaled(f.s, sa)
                    xb.set(f.s).scale(ca).addScaled(f.u, -sa)
                    rot.setFromColumns(xb, yb, f.f)
                    val sideOff = s * (row[SidePlates.OUT] * r * 0.95f + (if (j < 2) 0.05f else 0f))
                    val upOff = row[SidePlates.UP] * r
                    val n = SidePlates.index(i, j, s)
                    out.setTransform(
                        PartFamily.SIDE_PLATE, n,
                        f.p.x + f.s.x * sideOff + f.u.x * upOff,
                        f.p.y + f.s.y * sideOff + f.u.y * upOff,
                        f.p.z + f.s.z * sideOff + f.u.z * upOff,
                        rot, row[SidePlates.WIDTH] * r, row[SidePlates.HEIGHT] * r, depth[i] * row[SidePlates.DEPTH],
                    )
                    val bv = input.bands[SidePlates.band(i, j, s)]
                    palette.palAt(segU[i] + 0.03f * s, color)
                    emit(out, PartFamily.SIDE_PLATE, n, color, 0.25f + 1.9f * bv + 0.7f * w)
                }
            }
        }
    }

    private fun writeBellyLights(input: InputSnapshot, out: PoseBuffer) {
        for (k in 0 until DragonConfig.BELLY_LIGHTS) {
            val u = 0.06f + (k / 35f) * 0.88f
            spine.frameAt(u, frame)
            val r = BodyLayout.radiusAt(u)
            val down = 0.87f * r + 0.02f
            rot.setFromColumns(frame.s, frame.u, frame.f)
            out.setTransform(
                PartFamily.BELLY_LIGHT, k,
                frame.p.x - down * frame.u.x, frame.p.y - down * frame.u.y, frame.p.z - down * frame.u.z,
                rot, 0.95f * r, 0.035f, 0.1f,
            )
            emit(out, PartFamily.BELLY_LIGHT, k, palette.accent, 0.1f + 3.0f * waves.waveAt(u) + 0.2f * input.bass)
        }
    }

    private fun writeTowers(input: InputSnapshot, out: PoseBuffer) {
        val t = towers
        t.layout(frames, radius, depth)
        TowerClearance.solve(t, frames)
        for (n in 0 until t.count) towerV[n] = input.bands[n]
        t.size(towerV, radius)
        val accent = palette.accent
        val pulse = input.beatPulse * flash.localPulseScale
        for (n in 0 until t.count) {
            val f = frames[n / 3]
            rot.setFromColumns(f.s, f.u, f.f)
            val v = towerV[n]
            val w1 = t.width[n]
            val h = t.height[n]
            val sp = t.spire[n]
            val bx = t.baseX[n]; val by = t.baseY[n]; val bz = t.baseZ[n]
            val ux = f.u.x; val uy = f.u.y; val uz = f.u.z

            out.setTransform(PartFamily.TOWER_BODY, n, bx, by, bz, rot, w1, h, w1)
            palette.palAt(n / 80f, color)
            val gain = 0.35f + 2.3f * v
            emit(out, PartFamily.TOWER_BODY, n, color, gain)

            out.setTransform(PartFamily.SPIRE, n, bx + h * ux, by + h * uy, bz + h * uz, rot, 0.016f, sp, 0.016f)
            emit(out, PartFamily.SPIRE, n, color, gain * 1.4f)

            val capH = h + 0.14f
            out.setTransform(PartFamily.CAP, n, bx + capH * ux, by + capH * uy, bz + capH * uz, rot, 1.25f * w1, 0.025f, 1.25f * w1)
            emit(out, PartFamily.CAP, n, accent, 0.8f + 1.8f * v)

            val top = h + sp
            val br = 0.045f + 0.05f * v
            out.setTransform(PartFamily.BEACON, n, bx + top * ux, by + top * uy, bz + top * uz, rot, br, br, br)
            emit(out, PartFamily.BEACON, n, accent, 0.6f + 3f * v + pulse)
        }
    }

    private fun writeCore(input: InputSnapshot, out: PoseBuffer) {
        val core = out.fibers.core
        val s = spine.samples
        val steps = spine.steps
        for (k in 0..steps) {
            val u = k.toFloat() / steps
            core.xyz[k * 3] = s[k * 3]
            core.xyz[k * 3 + 1] = s[k * 3 + 1]
            core.xyz[k * 3 + 2] = s[k * 3 + 2]
            core.radius[k] = 0.52f * BodyLayout.radiusAt(u)
            val band = input.bands[min(DragonConfig.BANDS - 1, floor(u * DragonConfig.BANDS).toInt())]
            palette.palAt(u, color)
            val g = (0.25f + 2.4f * waves.waveAt(u) + 0.9f * band) * flashMul
            core.rgb[k * 3] = color.r * g
            core.rgb[k * 3 + 1] = color.g * g
            core.rgb[k * 3 + 2] = color.b * g
        }
    }
}
