package com.technomancer.neonlong.logic.audio

import java.util.concurrent.atomic.AtomicInteger

/**
 * Lock-free single-writer / single-reader handoff. The writer fills [writeSlot] and calls
 * [publish]; the reader calls [read] to get the newest published value. Neither side ever
 * waits or sees a half-written value, and nothing is allocated after construction.
 */
class TripleBuffer<T>(factory: () -> T) {
    private val slots = listOf(factory(), factory(), factory())
    // Index of the slot shared between writer and reader, with bit 2 set when it's fresh.
    private val middle = AtomicInteger(1)
    private var writeIndex = 0
    private var readIndex = 2

    /** The slot the writer may fill. */
    val writeSlot: T get() = slots[writeIndex]

    /** Makes the write slot visible to the reader and hands the writer a free slot. */
    fun publish() {
        val prev = middle.getAndSet(writeIndex or FRESH)
        writeIndex = prev and INDEX_MASK
    }

    /** Newest published value (the same one again if nothing new was published). */
    fun read(): T {
        if (middle.get() and FRESH != 0) {
            val prev = middle.getAndSet(readIndex)
            readIndex = prev and INDEX_MASK
        }
        return slots[readIndex]
    }

    private companion object {
        const val FRESH = 4
        const val INDEX_MASK = 3
    }
}
