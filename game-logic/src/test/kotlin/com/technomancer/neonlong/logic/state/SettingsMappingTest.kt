package com.technomancer.neonlong.logic.state

import com.technomancer.neonlong.logic.audio.FeatureExtractor
import com.technomancer.neonlong.logic.dragon.DragonSimulation
import com.technomancer.neonlong.logic.input.InputSnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SettingsMappingTest {
    @Test
    fun eachSettingReachesItsTarget() {
        val sim = DragonSimulation()
        val input = InputSnapshot()
        val features = FeatureExtractor()
        val s = Settings(palette = 2, sensitivity = 1.5f, reduceMotion = true, reduceFlashing = true)
        SettingsModel.applyToSimulation(s, sim, input)
        SettingsModel.applyToAnalysis(s, features)
        assertEquals(2, sim.palette.index)
        assertTrue(sim.flash.reduceFlashing)
        assertTrue(input.reducedMotion)
        assertEquals(1.5f, features.sensitivity, 1e-6f)

        SettingsModel.applyToSimulation(Settings(), sim, input)
        assertEquals(0, sim.palette.index)
        assertFalse(sim.flash.reduceFlashing)
        assertFalse(input.reducedMotion)
    }

    @Test
    fun sensitivityIsClampedAndSnapped() {
        assertEquals(0.5f, SettingsModel.snapSensitivity(0.1f), 1e-6f)
        assertEquals(2.0f, SettingsModel.snapSensitivity(3.7f), 1e-6f)
        assertEquals(1.05f, SettingsModel.snapSensitivity(1.04f), 1e-6f)
        assertEquals(1.25f, SettingsModel.snapSensitivity(1.26f), 1e-6f)
        assertEquals(1f, SettingsModel.snapSensitivity(Float.NaN), 1e-6f)
    }

    @Test
    fun sanitizeKeepsValuesInRange() {
        val s = SettingsModel.sanitize(Settings(palette = 9, sensitivity = 0.33f, volumeWidth = 10f), 3, 0.69f, 4.17f)
        assertEquals(2, s.palette)
        assertEquals(0.5f, s.sensitivity, 1e-6f)
        assertEquals(4.17f, s.volumeWidth, 1e-6f)
        assertEquals(0f, SettingsModel.sanitize(Settings(volumeWidth = 0f), 3, 0.69f, 4.17f).volumeWidth)
        assertEquals(0, SettingsModel.sanitize(Settings(palette = -1), 3, 0.69f, 4.17f).palette)
    }

    @Test
    fun sensitivityLabels() {
        assertEquals("1.0×", SettingsModel.sensitivityLabel(1f))
        assertEquals("1.5×", SettingsModel.sensitivityLabel(1.5f))
        assertEquals("1.25×", SettingsModel.sensitivityLabel(1.25f))
        assertEquals("0.55×", SettingsModel.sensitivityLabel(0.55f))
        assertEquals("2.0×", SettingsModel.sensitivityLabel(2f))
    }
}
