package com.technomancer.neonlong.logic

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionTest {
    @Test
    fun isSemver() {
        assertTrue(Regex("""\d+\.\d+\.\d+""").matches(LOGIC_VERSION), "LOGIC_VERSION=$LOGIC_VERSION")
    }
}
