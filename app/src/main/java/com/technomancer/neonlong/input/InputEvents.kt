package com.technomancer.neonlong.input

/**
 * Small fixed-size queue of discrete input events (taps, gestures, button presses) passed
 * from gesture callbacks and tracking threads to the frame Systems. Drained once per frame.
 */
class InputEvents(capacity: Int = 64) {
    private val kinds = IntArray(capacity)
    private val args = IntArray(capacity)
    private var head = 0
    private var size = 0
    private val lock = Any()

    fun post(kind: Int, arg: Int = 0) = synchronized(lock) {
        if (size == kinds.size) return@synchronized // full: drop, the user can repeat it
        val i = (head + size) % kinds.size
        kinds[i] = kind
        args[i] = arg
        size++
    }

    /** Calls [handler] for each queued event, oldest first, then empties the queue. */
    inline fun drain(handler: (kind: Int, arg: Int) -> Unit) {
        while (true) {
            val packed = poll()
            if (packed == EMPTY) return
            handler((packed ushr 32).toInt(), packed.toInt())
        }
    }

    /** Next event packed as (kind shl 32) or arg, or [EMPTY]. */
    fun poll(): Long = synchronized(lock) {
        if (size == 0) return@synchronized EMPTY
        val kind = kinds[head]
        val arg = args[head]
        head = (head + 1) % kinds.size
        size--
        (kind.toLong() shl 32) or (arg.toLong() and 0xffffffffL)
    }

    companion object {
        const val EMPTY = Long.MIN_VALUE

        const val SEGMENT_TOUCH = 1
        const val CALL = 2
        const val SEND = 3
        const val FLASH = 4
        const val PLAY_PAUSE = 6
        const val RESTART = 7
        const val FILE_READY = 8
        const val USE_DEMO = 9
    }
}
