package com.technomancer.neonlong.audio

import android.content.Context
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.logic.audio.AnalysisPipeline
import com.technomancer.neonlong.logic.audio.DecodedAudio
import com.technomancer.neonlong.logic.audio.SpectrumSnapshot
import com.technomancer.neonlong.logic.audio.TripleBuffer
import com.technomancer.neonlong.logic.audio.WavReader
import com.technomancer.neonlong.logic.config.AudioConfig

/**
 * Owns the music source and the analysis thread. The analysis runs at a fixed rate and
 * publishes a [SpectrumSnapshot] per hop: from the demo track while it plays (the window
 * ends at the sample being heard), otherwise from the idle breathing generator.
 */
class AudioEngine(private val context: Context, private val config: AudioConfig) {
    enum class State { IDLE, PLAYING, PAUSED }

    val snapshots = TripleBuffer { SpectrumSnapshot() }

    @Volatile var state = State.IDLE
        private set

    @Volatile private var demo: DecodedAudio? = null
    @Volatile private var player: SpatialDemoPlayer? = null

    /** Average analysis cost per hop over the last reporting window, in microseconds. */
    @Volatile var analysisAvgUs = 0L
        private set
    @Volatile var analysisMaxUs = 0L
        private set

    private val analysis = Thread(::analysisLoop, "NeonLong-analysis").apply { isDaemon = true }

    fun start() {
        if (!analysis.isAlive) analysis.start()
    }

    /** Badge / Play button: starts the demo the first time, then toggles pause. */
    fun togglePlay(dragonRoot: Entity?) {
        when (state) {
            State.PLAYING -> pause()
            else -> play(dragonRoot)
        }
    }

    fun play(dragonRoot: Entity?) {
        val p = player ?: createPlayer(dragonRoot) ?: return
        p.play()
        state = State.PLAYING
        Log.i(LOG_TAG, "[audio] demo playing")
    }

    fun pause() {
        if (state != State.PLAYING) return
        player?.pause()
        state = State.PAUSED
        Log.i(LOG_TAG, "[audio] demo paused")
    }

    /** The dragon was hidden or the app paused: pause, and stay paused until Play. */
    fun onHidden() {
        if (state == State.PLAYING) {
            pause()
            Log.i(LOG_TAG, "[audio] paused because the dragon is hidden")
        }
    }

    fun release() {
        player?.release()
        player = null
        state = State.IDLE
    }

    private fun createPlayer(dragonRoot: Entity?): SpatialDemoPlayer? = try {
        val audio = demo ?: context.assets.open("audio/demo_104bpm.wav").use { WavReader.read(it) }.also { demo = it }
        SpatialDemoPlayer(audio.samples, audio.sampleRate, dragonRoot).also { player = it }
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "[audio] can't start the demo: ${e.message}")
        null
    }

    private fun analysisLoop() {
        val pipeline = AnalysisPipeline(48_000, config)
        val periodNs = (1_000_000_000.0 / config.analysisRateHz).toLong()
        val dt = 1f / config.analysisRateHz
        var next = System.nanoTime()
        var t = 0f
        var sumNs = 0L
        var maxNs = 0L
        var hops = 0
        var wasPlaying = false
        while (true) {
            val start = System.nanoTime()
            val p = player
            val audio = demo
            val out = snapshots.writeSlot
            if (state == State.PLAYING && p != null && audio != null) {
                if (!wasPlaying) pipeline.reset()
                val end = p.positionIndex()
                pipeline.hopAudio(audio.samples, end, dt, end.toDouble() / audio.sampleRate, out)
                wasPlaying = true
            } else {
                pipeline.hopIdle(t, dt, out)
                wasPlaying = false
            }
            snapshots.publish()
            t += dt

            val cost = System.nanoTime() - start
            sumNs += cost
            if (cost > maxNs) maxNs = cost
            hops++
            if (hops == REPORT_HOPS) {
                analysisAvgUs = sumNs / hops / 1000
                analysisMaxUs = maxNs / 1000
                sumNs = 0; maxNs = 0; hops = 0
            }

            next += periodNs
            val sleep = next - System.nanoTime()
            if (sleep > 0) Thread.sleep(sleep / 1_000_000, (sleep % 1_000_000).toInt()) else next = System.nanoTime()
        }
    }

    private companion object {
        const val REPORT_HOPS = 300
    }
}
