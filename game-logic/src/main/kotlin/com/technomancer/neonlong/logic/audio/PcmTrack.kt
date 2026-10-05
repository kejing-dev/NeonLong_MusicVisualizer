package com.technomancer.neonlong.logic.audio

/**
 * A whole piece of mono audio held in memory, played as a loop. The demo is stored as floats;
 * the user's files are stored as 16-bit samples, which take half the memory.
 */
class PcmTrack private constructor(
    private val floats: FloatArray?,
    private val shorts: ShortArray?,
    /** Number of samples. */
    val size: Int,
    val sampleRate: Int,
) {
    val durationS: Double get() = size.toDouble() / sampleRate

    /** Copies [n] samples starting at [pos] into [out], wrapping around the end of the loop. */
    fun read(pos: Int, out: FloatArray, n: Int) {
        var p = Math.floorMod(pos, size)
        for (i in 0 until n) {
            out[i] = sample(p)
            p++
            if (p == size) p = 0
        }
    }

    /** Fills [out] with the samples just before [end], wrapping around the start of the loop. */
    fun window(end: Int, out: FloatArray) = read(end - out.size, out, out.size)

    private fun sample(i: Int): Float = floats?.get(i) ?: (shorts!![i] * SHORT_SCALE)

    companion object {
        private const val SHORT_SCALE = 1f / 32768f

        fun ofFloats(samples: FloatArray, sampleRate: Int) = PcmTrack(samples, null, samples.size, sampleRate)

        /** The first [count] entries of [samples] (the array may be longer than the audio). */
        fun ofShorts(samples: ShortArray, count: Int, sampleRate: Int) = PcmTrack(null, samples, count, sampleRate)
    }
}
