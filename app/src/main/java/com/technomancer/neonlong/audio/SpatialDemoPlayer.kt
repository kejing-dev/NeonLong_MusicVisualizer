package com.technomancer.neonlong.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.pico.spatial.audio.SpatialAudioTrackExtension
import com.pico.spatial.core.ecs.Entity
import com.technomancer.neonlong.app.LOG_TAG

/**
 * Loops a mono float PCM buffer through an Android AudioTrack attached to [source] with the
 * SDK's spatial-audio extension, so the music comes from the dragon. Unlike the SDK's own
 * audio player, AudioTrack reports exactly which sample is playing, which lets the visuals
 * follow what's audible. If the extension can't attach, it plays as normal stereo audio.
 */
class SpatialDemoPlayer(private val samples: FloatArray, private val sampleRate: Int, source: Entity?) {
    private val extension = SpatialAudioTrackExtension()
    private val track: AudioTrack
    /** True if the track is spatialized from the dragon. */
    val spatial: Boolean

    @Volatile private var running = true
    /** Set by [restart]; the writer starts again from the first sample. */
    @Volatile private var restartRequested = false
    /** Playback head at the last restart, so positions count from there. */
    @Volatile private var headBase = 0L
    @Volatile private var playing = false
    private val lock = Object()
    private val writer: Thread

    init {
        val minBytes = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT)
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(minBytes * 2, CHUNK * 4 * 4))
        var ok = false
        if (source != null) {
            try {
                extension.spatialAudioTrackExtensionConfig(SpatialAudioTrackExtension.SpatialAudioMode.OBJECT, false, builder)
                extension.attachToEntityWithBuilder(source, builder)
                ok = true
            } catch (e: Throwable) {
                Log.w(LOG_TAG, "[audio] spatial audio unavailable, playing plain: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
        spatial = ok
        track = builder.build()
        writer = Thread(::writeLoop, "NeonLong-demo-writer").apply { isDaemon = true; start() }
        Log.i(LOG_TAG, "[audio] demo player ready: ${samples.size} samples at $sampleRate Hz, spatial=$spatial")
    }

    fun play() {
        track.play()
        synchronized(lock) {
            playing = true
            lock.notifyAll()
        }
    }

    fun pause() {
        playing = false
        track.pause()
    }

    val isPlaying: Boolean get() = playing

    /** Drops what's queued and continues from the first sample, playing or paused as before. */
    fun restart() {
        val wasPlaying = playing
        playing = false
        try { track.pause(); track.flush() } catch (_: Throwable) {}
        headBase = track.playbackHeadPosition.toLong() and 0xffffffffL
        restartRequested = true
        if (wasPlaying) play()
    }

    /** Index into the loop of the sample being heard now. */
    fun positionIndex(): Int {
        val head = (track.playbackHeadPosition.toLong() and 0xffffffffL) - headBase
        return Math.floorMod(head, samples.size.toLong()).toInt()
    }

    fun release() {
        running = false
        synchronized(lock) { lock.notifyAll() }
        try { track.pause(); track.flush() } catch (_: Throwable) {}
        writer.join(200)
        track.release()
        try { extension.release() } catch (_: Throwable) {}
    }

    /** Feeds the loop into the track forever, blocking while paused. */
    private fun writeLoop() {
        val chunk = FloatArray(CHUNK)
        var cursor = 0
        while (running) {
            synchronized(lock) {
                while (running && !playing) lock.wait()
            }
            if (!running) break
            if (restartRequested) {
                restartRequested = false
                cursor = 0
            }
            for (i in 0 until CHUNK) {
                chunk[i] = samples[cursor]
                cursor++
                if (cursor == samples.size) cursor = 0
            }
            var off = 0
            while (off < CHUNK && running) {
                val n = track.write(chunk, off, CHUNK - off, AudioTrack.WRITE_BLOCKING)
                if (n < 0) {
                    Log.w(LOG_TAG, "[audio] write failed: $n")
                    return
                }
                off += n
            }
        }
    }

    private companion object {
        const val CHUNK = 1024
    }
}
