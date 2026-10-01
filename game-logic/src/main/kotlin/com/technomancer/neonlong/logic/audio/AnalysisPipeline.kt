package com.technomancer.neonlong.logic.audio

import com.technomancer.neonlong.logic.config.AudioConfig
import com.technomancer.neonlong.logic.config.DragonConfig

/**
 * One analysis hop: spectrum → 81 bands → smoothed features → beat, written into a
 * [SpectrumSnapshot]. With no audio, the idle generator fills the bands instead and no beats
 * fire. Runs on the analysis thread; allocates nothing per hop.
 */
class AnalysisPipeline(sampleRate: Int, private val c: AudioConfig = AudioConfig()) {
    val analyzer = SpectrumAnalyzer(c)
    private val mapper = BandMapper(sampleRate, c.fftSize)
    val features = FeatureExtractor(c)
    val beats = BeatDetector(c)
    private val idle = IdleGenerator(c.idleBpm)
    private val raw = FloatArray(DragonConfig.BANDS)
    private var beatCount = 0L
    private var lastBeat = 0f
    private var sequence = 0L

    /** Analyzes the window of [src] ending at index [end] (wrapping for loops). */
    fun hopAudio(src: FloatArray, end: Int, dt: Float, positionS: Double, out: SpectrumSnapshot) {
        analyzer.analyze(src, end)
        finishAudioHop(dt, positionS, out)
    }

    /** Analyzes an already-gathered window of exactly fftSize samples. */
    fun hopWindow(window: FloatArray, dt: Float, positionS: Double, out: SpectrumSnapshot) {
        analyzer.analyzeWindow(window)
        finishAudioHop(dt, positionS, out)
    }

    private fun finishAudioHop(dt: Float, positionS: Double, out: SpectrumSnapshot) {
        mapper.map(analyzer.normalized, raw)
        features.update(raw, dt)
        val beat = beats.update(features.targetMean(0, 9), dt)
        if (beat > 0f) { beatCount++; lastBeat = beat }
        write(out, beat, hasAudio = true, positionS = positionS)
    }

    /** Idle breathing at time [t]; never beats. */
    fun hopIdle(t: Float, dt: Float, out: SpectrumSnapshot) {
        idle.fill(t, raw)
        features.setDirect(raw)
        beats.update(0f, dt)
        write(out, 0f, hasAudio = false, positionS = 0.0)
    }

    fun reset() {
        analyzer.reset()
        features.reset()
        beats.reset()
    }

    private fun write(out: SpectrumSnapshot, beat: Float, hasAudio: Boolean, positionS: Double) {
        System.arraycopy(features.bands, 0, out.bands, 0, out.bands.size)
        out.bass = features.bass
        out.mid = features.mid
        out.high = features.high
        out.vol = features.vol
        out.beatStrength = beat
        out.beatCount = beatCount
        out.lastBeatStrength = lastBeat
        out.beatPulse = beats.pulse
        out.hasAudio = hasAudio
        out.positionS = positionS
        out.sequence = ++sequence
    }
}
