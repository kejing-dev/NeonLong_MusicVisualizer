package com.technomancer.neonlong.logic.math

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RngParityTest {
    private fun check(seed: Int, expected: DoubleArray) {
        val rng = Rng(seed)
        for (i in expected.indices) {
            assertEquals(expected[i], rng.next(), 1e-9, "seed $seed output $i")
        }
    }

    @Test
    fun seed3() = check(3, doubleArrayOf(0.7202267838, 0.0386621605, 0.4561921926, 0.0749280096, 0.7630053898))

    @Test
    fun seed7() = check(7, doubleArrayOf(0.0117047532, 0.0619582576, 0.9769076328, 0.6990287057, 0.5214452685))

    @Test
    fun seed1234() = check(1234, doubleArrayOf(0.0732949781, 0.7034119898, 0.9028560191, 0.9705493662, 0.0409639762))
}
