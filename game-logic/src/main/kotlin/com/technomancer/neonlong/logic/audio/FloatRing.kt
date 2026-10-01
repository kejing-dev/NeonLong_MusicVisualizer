package com.technomancer.neonlong.logic.audio

import java.util.concurrent.atomic.AtomicLong

/**
 * Single-producer / single-consumer ring of mono float samples. The producer [write]s;
 * the consumer reads the newest samples with [readLatest]. When the producer laps the
 * consumer, the oldest samples are simply overwritten (visuals only need recent audio).
 */
class FloatRing(capacity: Int = 16384) {
    private val buf = FloatArray(capacity)
    private val size = capacity
    /** Total samples ever written. */
    private val written = AtomicLong(0)

    val totalWritten: Long get() = written.get()

    fun write(src: FloatArray, offset: Int, count: Int) {
        var w = written.get()
        for (i in 0 until count) {
            buf[(w % size).toInt()] = src[offset + i]
            w++
        }
        written.set(w)
    }

    /**
     * Copies the newest [count] samples into [dst] (oldest first). Missing history is zero.
     * Returns the total-written position the copy ends at.
     */
    fun readLatest(dst: FloatArray, count: Int): Long {
        val end = written.get()
        val start = end - count
        for (i in 0 until count) {
            val p = start + i
            dst[i] = if (p < 0 || p < end - size) 0f else buf[(p % size).toInt()]
        }
        return end
    }
}
