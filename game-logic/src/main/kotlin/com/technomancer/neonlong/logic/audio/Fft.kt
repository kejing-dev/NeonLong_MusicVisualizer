package com.technomancer.neonlong.logic.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * In-place iterative radix-2 FFT of size [n] (a power of two). Twiddles and the bit-reversal
 * table are computed once; [forward] allocates nothing.
 */
class Fft(val n: Int) {
    private val cosT = FloatArray(n / 2)
    private val sinT = FloatArray(n / 2)
    private val rev = IntArray(n)

    init {
        require(n >= 2 && n and (n - 1) == 0) { "FFT size must be a power of two: $n" }
        for (k in 0 until n / 2) {
            val a = -2.0 * PI * k / n
            cosT[k] = cos(a).toFloat()
            sinT[k] = sin(a).toFloat()
        }
        var bits = 0
        while (1 shl bits < n) bits++
        for (i in 0 until n) {
            var r = 0
            var x = i
            for (b in 0 until bits) {
                r = (r shl 1) or (x and 1)
                x = x shr 1
            }
            rev[i] = r
        }
    }

    /** Transforms ([re], [im]) in place. */
    fun forward(re: FloatArray, im: FloatArray) {
        for (i in 0 until n) {
            val j = rev[i]
            if (j > i) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var size = 2
        while (size <= n) {
            val half = size / 2
            val step = n / size
            var start = 0
            while (start < n) {
                var k = 0
                for (j in start until start + half) {
                    val wr = cosT[k]; val wi = sinT[k]
                    val l = j + half
                    val xr = re[l] * wr - im[l] * wi
                    val xi = re[l] * wi + im[l] * wr
                    re[l] = re[j] - xr; im[l] = im[j] - xi
                    re[j] += xr; im[j] += xi
                    k += step
                }
                start += size
            }
            size *= 2
        }
    }
}
