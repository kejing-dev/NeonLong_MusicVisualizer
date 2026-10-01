package com.technomancer.neonlong.logic.audio

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Decoded audio: mono samples in [-1, 1] and their rate. */
class DecodedAudio(val samples: FloatArray, val sampleRate: Int)

/** Reads 16-bit PCM or 32-bit float WAV files and mixes them down to mono. */
object WavReader {
    fun read(input: InputStream): DecodedAudio {
        val bytes = input.readBytes()
        val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WAVE") { "not a WAV file" }
        var pos = 12
        var format = 0
        var channels = 0
        var rate = 0
        var bits = 0
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4)
            val len = b.getInt(pos + 4)
            val body = pos + 8
            when (id) {
                "fmt " -> {
                    format = b.getShort(body).toInt()
                    channels = b.getShort(body + 2).toInt()
                    rate = b.getInt(body + 4)
                    bits = b.getShort(body + 14).toInt()
                }
                "data" -> {
                    val frameBytes = channels * bits / 8
                    val frames = minOf(len, bytes.size - body) / frameBytes
                    val out = FloatArray(frames)
                    for (f in 0 until frames) {
                        var sum = 0f
                        for (c in 0 until channels) {
                            val o = body + f * frameBytes + c * bits / 8
                            sum += when {
                                format == 1 && bits == 16 -> b.getShort(o) / 32768f
                                format == 3 && bits == 32 -> b.getFloat(o)
                                else -> error("unsupported WAV format $format / $bits-bit")
                            }
                        }
                        out[f] = sum / channels
                    }
                    return DecodedAudio(out, rate)
                }
            }
            pos = body + len + (len and 1)
        }
        error("WAV has no data chunk")
    }
}
