package com.technomancer.neonlong.audio.sources

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import com.technomancer.neonlong.logic.audio.PcmTrack
import java.io.File
import java.nio.ByteOrder

/** One of the user's music files, decoded into memory. */
class LoadedFile(val track: PcmTrack, val title: String)

/**
 * Decodes a music file the user picked (MP3, AAC, FLAC, OGG, WAV: anything the device's
 * decoders handle) into mono 16-bit audio in memory. The audio is only ever held in memory
 * for playing and for the visuals; nothing is written anywhere. Call off the main thread.
 */
object FileSource {
    /** Decodes [uri]. Throws if it isn't playable audio; at most [maxSeconds] are kept. */
    fun decode(context: Context, uri: Uri, maxSeconds: Int): LoadedFile {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IllegalArgumentException("no audio in the file")
            extractor.selectTrack(trackIndex)
            val input = extractor.getTrackFormat(trackIndex)
            val mime = input.getString(MediaFormat.KEY_MIME)!!
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(input, null, null, 0)
            codec.start()

            var sampleRate = input.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = input.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var floatPcm = false
            val durationUs = if (input.containsKey(MediaFormat.KEY_DURATION)) input.getLong(MediaFormat.KEY_DURATION) else 0L
            val limit = maxSeconds.toLong() * sampleRate
            val guess = if (durationUs > 0) durationUs * sampleRate / 1_000_000 + sampleRate else 60L * sampleRate
            var pcm = ShortArray(minOf(guess, limit).toInt().coerceAtLeast(sampleRate))
            var count = 0

            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val buffer = codec.getInputBuffer(inIndex)!!
                        val n = extractor.readSampleData(buffer, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = codec.outputFormat
                        sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        floatPcm = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    }
                    outIndex >= 0 -> {
                        val buffer = codec.getOutputBuffer(outIndex)!!.order(ByteOrder.nativeOrder())
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        val frames = info.size / (channels * if (floatPcm) 4 else 2)
                        val keep = minOf(frames.toLong(), limit - count).toInt()
                        if (count + keep > pcm.size) pcm = pcm.copyOf(maxOf(pcm.size * 2, count + keep).coerceAtMost(limit.toInt()))
                        // Mix every channel down to one.
                        if (floatPcm) {
                            val src = buffer.asFloatBuffer()
                            for (i in 0 until keep) {
                                var sum = 0f
                                for (c in 0 until channels) sum += src.get(i * channels + c)
                                pcm[count + i] = (sum / channels * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                            }
                        } else {
                            val src = buffer.asShortBuffer()
                            for (i in 0 until keep) {
                                var sum = 0
                                for (c in 0 until channels) sum += src.get(i * channels + c)
                                pcm[count + i] = (sum / channels).toShort()
                            }
                        }
                        count += keep
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 || count >= limit) outputDone = true
                    }
                }
            }
            if (count < sampleRate / 10) throw IllegalArgumentException("the file has no audio to play")
            return LoadedFile(PcmTrack.ofShorts(pcm, count, sampleRate), title(context, uri))
        } finally {
            try { codec?.stop() } catch (_: Throwable) {}
            try { codec?.release() } catch (_: Throwable) {}
            extractor.release()
        }
    }

    /** The file's display name without its extension. */
    fun title(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) name = c.getString(0)
                }
            } catch (_: Exception) {
            }
        }
        return (name ?: uri.lastPathSegment ?: "").substringAfterLast('/').substringBeforeLast('.')
    }

    /** The app's own Music folder: files copied here can be played without a file picker. */
    fun musicFolder(context: Context): File? = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)

    /** Audio files in [musicFolder], by name. */
    fun listMusicFolder(context: Context): List<File> =
        musicFolder(context)?.listFiles { f -> f.isFile && f.extension.lowercase() in EXTENSIONS }?.sortedBy { it.name.lowercase() } ?: emptyList()

    private val EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav")
    private const val TIMEOUT_US = 10_000L
}
