package com.technomancer.neonlong.tools.demosynth

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/** Writes mono 16-bit PCM WAV files. */
object WavWriter {
    fun write(file: File, samples: FloatArray, sampleRate: Int) {
        val dataBytes = samples.size * 2
        val buf = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()); buf.putInt(36 + dataBytes); buf.put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()); buf.putInt(16)
        buf.putShort(1) // PCM
        buf.putShort(1) // mono
        buf.putInt(sampleRate)
        buf.putInt(sampleRate * 2)
        buf.putShort(2) // block align
        buf.putShort(16)
        buf.put("data".toByteArray()); buf.putInt(dataBytes)
        for (s in samples) {
            val v = (s.coerceIn(-1f, 1f) * 32767f).roundToInt()
            buf.putShort(v.toShort())
        }
        file.parentFile?.mkdirs()
        file.writeBytes(buf.array())
    }
}
