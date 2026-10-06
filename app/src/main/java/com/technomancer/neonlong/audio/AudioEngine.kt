package com.technomancer.neonlong.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.audio.sources.FileSource
import com.technomancer.neonlong.audio.sources.LoadedFile
import com.technomancer.neonlong.logic.audio.AnalysisPipeline
import com.technomancer.neonlong.logic.audio.PcmTrack
import com.technomancer.neonlong.logic.audio.SpectrumSnapshot
import com.technomancer.neonlong.logic.audio.TripleBuffer
import com.technomancer.neonlong.logic.audio.WavReader
import com.technomancer.neonlong.logic.config.AudioConfig
import com.technomancer.neonlong.logic.state.Settings
import com.technomancer.neonlong.logic.state.SettingsModel

/**
 * Owns the music source and the analysis thread. The music is either the built-in demo track
 * or one of the user's files, held in memory and played as a loop from the dragon. The
 * analysis runs at a fixed rate and publishes a [SpectrumSnapshot] per hop: from the music
 * while it plays (the window ends at the sample being heard), otherwise from the idle
 * breathing generator.
 */
class AudioEngine(
    private val context: Context,
    private val config: AudioConfig,
    /** The user's settings, read by the analysis thread for the band sensitivity. */
    private val settings: () -> Settings,
) {
    enum class State { IDLE, PLAYING, PAUSED }
    enum class Source { DEMO, FILE }

    /** A player and the audio it plays, swapped together so the analysis never mixes two tracks. */
    private class Playing(val player: SpatialDemoPlayer, val track: PcmTrack)

    val snapshots = TripleBuffer { SpectrumSnapshot() }

    @Volatile var state = State.IDLE
        private set

    /** Which music is selected. */
    @Volatile var source = Source.DEMO
        private set

    /** Name of the user's file while [source] is FILE. */
    @Volatile var fileTitle: String? = null
        private set

    /** True while a picked file is being decoded. */
    @Volatile var loading = false
        private set

    private var demo: PcmTrack? = null
    @Volatile private var selected: PcmTrack? = null
    @Volatile private var playing: Playing? = null
    @Volatile private var pending: LoadedFile? = null

    /** Length of the selected music in seconds, once it has been loaded. */
    @Volatile var durationS = 0.0
        private set

    /** Where playback is in the loop, in seconds. */
    @Volatile var positionS = 0.0
        private set

    /** Average analysis cost per hop over the last reporting window, in microseconds. */
    @Volatile var analysisAvgUs = 0L
        private set
    @Volatile var analysisMaxUs = 0L
        private set

    private val analysis = Thread(::analysisLoop, "NeonLong-analysis").apply { isDaemon = true }

    fun start() {
        if (!analysis.isAlive) analysis.start()
    }

    /** Play button: starts the selected music the first time, then toggles pause. */
    fun togglePlay(dragonRoot: Entity?) {
        when (state) {
            State.PLAYING -> pause()
            else -> play(dragonRoot)
        }
    }

    fun play(dragonRoot: Entity?) {
        val p = playing ?: createPlayer(dragonRoot) ?: return
        p.player.play()
        state = State.PLAYING
        Log.i(LOG_TAG, "[audio] ${source.name.lowercase()} playing")
    }

    /** Back to the start of the loop, keeping the play/pause state. */
    fun restart() {
        val p = playing ?: return
        p.player.restart()
        positionS = 0.0
        Log.i(LOG_TAG, "[audio] restarted")
    }

    fun pause() {
        if (state != State.PLAYING) return
        playing?.player?.pause()
        state = State.PAUSED
        Log.i(LOG_TAG, "[audio] paused")
    }

    /** The dragon was hidden or the app paused: pause, and stay paused until Play. */
    fun onHidden() {
        if (state == State.PLAYING) {
            pause()
            Log.i(LOG_TAG, "[audio] paused because the dragon is hidden")
        }
    }

    fun release() {
        stopPlayer()
    }

    /**
     * Decodes the picked file on a background thread. When it's ready, [onReady] is called
     * (from that thread) and the caller starts it with [startLoadedFile] on the frame thread;
     * if it can't be played, [onError] is called and the current music carries on.
     */
    fun loadFile(uri: Uri, onReady: () -> Unit, onError: () -> Unit) {
        if (loading) return
        loading = true
        Thread({
            try {
                val start = System.nanoTime()
                val file = FileSource.decode(context, uri, config.maxFileMinutes * 60)
                Log.i(
                    LOG_TAG,
                    "[audio] file decoded: ${"%.1f".format(file.track.durationS)} s at ${file.track.sampleRate} Hz " +
                        "in ${(System.nanoTime() - start) / 1_000_000} ms",
                )
                pending = file
                onReady()
            } catch (e: Throwable) {
                Log.w(LOG_TAG, "[audio] can't play that file: ${e.javaClass.simpleName}: ${e.message}")
                onError()
            } finally {
                loading = false
            }
        }, "NeonLong-file-decode").apply { isDaemon = true }.start()
    }

    /** Switches to the file decoded by [loadFile] and plays it. */
    fun startLoadedFile(dragonRoot: Entity?) {
        val file = pending ?: return
        pending = null
        stopPlayer()
        selected = file.track
        source = Source.FILE
        fileTitle = file.title
        durationS = file.track.durationS
        play(dragonRoot)
    }

    /** Switches back to the demo track, playing if music was playing. */
    fun useDemo(dragonRoot: Entity?) {
        if (source == Source.DEMO) return
        val wasPlaying = state == State.PLAYING
        stopPlayer()
        selected = null
        source = Source.DEMO
        fileTitle = null
        durationS = demo?.durationS ?: 0.0
        if (wasPlaying) play(dragonRoot)
        Log.i(LOG_TAG, "[audio] back to the demo track")
    }

    private fun stopPlayer() {
        val p = playing
        playing = null
        p?.player?.release()
        state = State.IDLE
        positionS = 0.0
    }

    private fun createPlayer(dragonRoot: Entity?): Playing? = try {
        val track = selected ?: demo ?: context.assets.open("audio/demo_104bpm.wav").use { WavReader.read(it) }
            .let { PcmTrack.ofFloats(it.samples, it.sampleRate) }
            .also { demo = it }
        durationS = track.durationS
        Playing(SpatialDemoPlayer(track, dragonRoot), track).also { playing = it }
    } catch (e: Throwable) {
        Log.e(LOG_TAG, "[audio] can't start playback: ${e.message}")
        null
    }

    private fun analysisLoop() {
        var pipeline = AnalysisPipeline(DEMO_RATE, config)
        var pipelineRate = DEMO_RATE
        val window = FloatArray(config.fftSize)
        val periodNs = (1_000_000_000.0 / config.analysisRateHz).toLong()
        val dt = 1f / config.analysisRateHz
        var next = System.nanoTime()
        var t = 0f
        var sumNs = 0L
        var maxNs = 0L
        var hops = 0
        var last: Playing? = null
        while (true) {
            val start = System.nanoTime()
            val p = playing
            val out = snapshots.writeSlot
            if (state == State.PLAYING && p != null) {
                if (p.track.sampleRate != pipelineRate) {
                    // Files come in several sample rates; the band mapping depends on it.
                    pipelineRate = p.track.sampleRate
                    pipeline = AnalysisPipeline(pipelineRate, config)
                    last = null
                }
                if (p !== last) pipeline.reset()
                SettingsModel.applyToAnalysis(settings(), pipeline.features)
                val end = p.player.positionIndex()
                val position = end.toDouble() / p.track.sampleRate
                positionS = position
                p.track.window(end, window)
                pipeline.hopWindow(window, dt, position, out)
                last = p
            } else {
                SettingsModel.applyToAnalysis(settings(), pipeline.features)
                pipeline.hopIdle(t, dt, out)
                last = null
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
        const val DEMO_RATE = 48_000
    }
}
