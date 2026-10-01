package com.technomancer.neonlong.tools.demosynth

/**
 * The demo track's score: 104 BPM, 16th-note steps, a 16-bar cycle in A minor pentatonic.
 * [render] plays [bars] bars into [voices]; [kickTimes] lists every kick it scheduled.
 */
class Sequencer(private val voices: Voices) {
    val kickTimes = ArrayList<Double>()

    fun render(bars: Int) {
        for (i in 0 until bars * 16) {
            val t = i * STEP_S
            val bar = i / 16
            val s = i % 16
            val b = bar % 16
            val ch = bar % 4
            val intro = b < 2
            val breakdown = b == 10 || b == 11

            if (s == 0 && (b == 0 || b == 8)) voices.gong(t)
            if (s == 0) {
                val notes = IntArray(3) { ROOTS[ch] + 12 + QUAL[ch][it] }
                voices.pad(t, notes, 16 * STEP_S)
            }
            if (s == 0 && b % 8 == 2) {
                for (k in 0 until 7) voices.pluck(t + 0.03 * k, SC[k + 2], 0.1)
            }
            if (!intro && !breakdown) {
                if (s % 4 == 0 || (s == 14 && bar % 2 == 1)) {
                    voices.kick(t)
                    kickTimes += t
                }
                if (s == 4 || s == 12 || (b == 15 && s >= 12)) voices.snare(t)
                voices.hat(t, open = s % 4 == 2)
                val offset = BASS[s]
                if (offset != null) voices.bass(t, ROOTS[ch] + offset, 1.6 * STEP_S)
            } else if (breakdown && s % 2 == 0) {
                voices.hat(t, open = false)
            }
            val mel = MEL[ch][s]
            if (mel >= 0) voices.pluck(t, SC[mel])
        }
    }

    companion object {
        const val BPM = 104.0
        const val STEP_S = 60.0 / BPM / 4.0

        val SC = intArrayOf(57, 60, 62, 64, 67, 69, 72, 74, 76, 79, 81)
        val ROOTS = intArrayOf(45, 41, 48, 43)
        val QUAL = arrayOf(intArrayOf(0, 3, 7), intArrayOf(0, 4, 7), intArrayOf(0, 4, 7), intArrayOf(0, 4, 7))
        val BASS = mapOf(0 to 0, 3 to 0, 6 to 12, 8 to 0, 10 to 7, 14 to 12)
        val MEL = arrayOf(
            intArrayOf(7, -1, 6, -1, 4, -1, 5, -1, 4, -1, 2, 3, -1, 4, -1, -1),
            intArrayOf(5, -1, 4, -1, 2, -1, 1, -1, 2, -1, 4, -1, 6, 5, 4, -1),
            intArrayOf(7, -1, 6, -1, 4, -1, 5, -1, 7, -1, 8, 9, -1, 8, -1, -1),
            intArrayOf(7, 8, 9, -1, 8, -1, 7, -1, 6, -1, 7, -1, 4, -1, -1, -1),
        )
    }
}
