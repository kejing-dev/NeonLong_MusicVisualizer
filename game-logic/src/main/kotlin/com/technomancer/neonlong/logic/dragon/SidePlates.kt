package com.technomancer.neonlong.logic.dragon

/**
 * Armor plates on the body's flanks: three rows per side per segment, each tilted and
 * offset from the body as listed in [ROWS].
 */
object SidePlates {
    /** Per row: up offset, height, depth fraction, tilt, out offset, width (all × body radius where relevant). */
    val ROWS = arrayOf(
        floatArrayOf(0.36f, 0.56f, 0.84f, 0.28f, 1.00f, 0.12f),
        floatArrayOf(-0.34f, 0.50f, 0.74f, -0.22f, 0.99f, 0.12f),
        floatArrayOf(0.86f, 0.14f, 0.90f, 0.55f, 0.74f, 0.50f),
    )
    const val UP = 0; const val HEIGHT = 1; const val DEPTH = 2; const val TILT = 3; const val OUT = 4; const val WIDTH = 5

    /** Instance index of the plate in row [j] on side [s] (−1 left, +1 right) of segment [i]. */
    fun index(i: Int, j: Int, s: Int): Int = i * 6 + j * 2 + (if (s > 0) 1 else 0)

    /** Band a plate reads. */
    fun band(i: Int, j: Int, s: Int): Int = 3 * i + ((j + (if (s > 0) 1 else 0)) % 3)
}
