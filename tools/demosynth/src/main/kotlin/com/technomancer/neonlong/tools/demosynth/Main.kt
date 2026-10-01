package com.technomancer.neonlong.tools.demosynth

import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

const val SAMPLE_RATE = 48_000

/** The rendered demo loop and the times of its kicks, in seconds from the loop start. */
class DemoTrack(val samples: FloatArray, val kicks: List<Double>) {
    val durationS: Double get() = samples.size.toDouble() / SAMPLE_RATE
}

/**
 * Renders 32 bars and keeps bars 16–31, so the loop starts with the previous cycle's tails
 * already ringing and repeats seamlessly. Peak-normalized to −1 dBFS.
 */
fun renderDemo(): DemoTrack {
    val stepSamples = Sequencer.STEP_S * SAMPLE_RATE
    val total = (32 * 16 * stepSamples).roundToInt() + SAMPLE_RATE * 6 // room for the last tails
    val voiceBus = FloatArray(total)
    val send = FloatArray(total)
    val seq = Sequencer(Voices(SAMPLE_RATE, voiceBus, send))
    seq.render(bars = 32)

    val bus = FloatArray(total)
    for (i in 0 until total) {
        bus[i] = voiceBus[i] * 0.62f
        send[i] *= 0.3f
    }
    Delay(SAMPLE_RATE, 0.433, 0.36, 2600.0).processInto(send, bus)
    Compressor(SAMPLE_RATE).process(bus)

    val start = (16 * 16 * stepSamples).roundToInt()
    val length = (16 * 16 * stepSamples).roundToInt()
    val loop = bus.copyOfRange(start, start + length)
    var peak = 0f
    for (v in loop) peak = maxOf(peak, abs(v))
    val gain = (10.0.pow(-1.0 / 20.0) / peak).toFloat()
    for (i in loop.indices) loop[i] *= gain

    val loopStartS = 16 * 16 * Sequencer.STEP_S
    val loopEndS = loopStartS + length.toDouble() / SAMPLE_RATE
    val kicks = seq.kickTimes.filter { it >= loopStartS - 1e-9 && it < loopEndS }.map { it - loopStartS }
    return DemoTrack(loop, kicks)
}

fun kicksJson(track: DemoTrack): String = buildString {
    append("{\n  \"bpm\": ${Sequencer.BPM},\n")
    append("  \"sampleRate\": $SAMPLE_RATE,\n")
    append("  \"durationS\": ${"%.6f".format(java.util.Locale.ROOT, track.durationS)},\n")
    append("  \"kicks\": [")
    append(track.kicks.joinToString(", ") { "%.6f".format(java.util.Locale.ROOT, it) })
    append("]\n}\n")
}

/** Interface sounds: an 8 ms 3.2 kHz blip at −24 dBFS and a 30 ms 1.6→2.4 kHz chirp at −18 dBFS. */
fun renderUiSounds(): Pair<FloatArray, FloatArray> {
    val hover = FloatArray((0.008 * SAMPLE_RATE).roundToInt())
    Voices(SAMPLE_RATE, hover, FloatArray(hover.size)).uiBlip(3200.0, 0.008, 10.0.pow(-24.0 / 20.0))
    val select = FloatArray((0.030 * SAMPLE_RATE).roundToInt())
    Voices(SAMPLE_RATE, select, FloatArray(select.size)).uiChirp(1600.0, 2400.0, 0.030, 10.0.pow(-18.0 / 20.0))
    return hover to select
}

/**
 * Args: <app assets dir> <test resources dir>.
 * Writes audio/demo_104bpm.wav and the UI sounds into the assets dir, and
 * demo_104bpm.wav + demo_104bpm.kicks.json into the test resources dir.
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "usage: demosynth <assetsDir> <testResourcesDir>" }
    val assets = File(args[0])
    val testRes = File(args[1])
    val track = renderDemo()
    WavWriter.write(File(assets, "audio/demo_104bpm.wav"), track.samples, SAMPLE_RATE)
    WavWriter.write(File(testRes, "demo_104bpm.wav"), track.samples, SAMPLE_RATE)
    File(testRes, "demo_104bpm.kicks.json").apply { parentFile.mkdirs() }.writeText(kicksJson(track))
    val (hover, select) = renderUiSounds()
    WavWriter.write(File(assets, "audio/GEN_ui_hover.wav"), hover, SAMPLE_RATE)
    WavWriter.write(File(assets, "audio/GEN_ui_select.wav"), select, SAMPLE_RATE)
    println("demosynth: %.3f s loop, %d kicks, first at %.3f s".format(track.durationS, track.kicks.size, track.kicks.firstOrNull() ?: -1.0))
}
