package com.technomancer.neonlong.tools.demosynth

/**
 * Feedback echo: input → delay line (with feedback) → lowpass → output.
 * Processes a whole send buffer and adds the wet result into [out].
 */
class Delay(
    private val sampleRate: Int,
    private val timeS: Double,
    private val feedback: Double,
    private val lowpassHz: Double,
) {
    fun processInto(send: FloatArray, out: FloatArray) {
        val length = (timeS * sampleRate).toInt()
        val line = DoubleArray(length)
        var pos = 0
        val lp = Biquad(sampleRate).set(Biquad.Type.LOWPASS, lowpassHz)
        for (i in send.indices) {
            val delayed = line[pos]
            line[pos] = send[i] + delayed * feedback
            pos = (pos + 1) % length
            out[i] += lp.process(delayed).toFloat()
        }
    }
}
