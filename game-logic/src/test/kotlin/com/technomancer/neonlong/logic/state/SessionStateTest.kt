package com.technomancer.neonlong.logic.state

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionStateTest {
    private fun visibleFocused() = SessionState().apply {
        onActivityResumed(); onContainerShown(); onContainerFocused()
    }

    @Test
    fun visibleStates() {
        val s = visibleFocused()
        assertEquals(SessionState.Visibility.VISIBLE_FOCUSED, s.visibility)
        assertTrue(s.running)
        s.onContainerUnfocused()
        assertEquals(SessionState.Visibility.VISIBLE_UNFOCUSED, s.visibility)
        assertTrue(s.running, "unfocused but visible keeps running")
    }

    @Test
    fun pauseStopsRunning() {
        val s = visibleFocused()
        s.onActivityPaused()
        assertFalse(s.running)
        assertEquals(SessionState.Visibility.HIDDEN, s.visibility)
    }

    @Test
    fun containerHiddenStopsRunning() {
        val s = visibleFocused()
        s.onContainerHidden()
        assertFalse(s.running)
    }

    @Test
    fun headsetRemovedStopsRunning() {
        val s = visibleFocused()
        s.onHeadsetWorn(false)
        assertFalse(s.running)
        s.onHeadsetWorn(true)
        assertTrue(s.running)
    }

    @Test
    fun resumeFromHiddenDoesNotRecreateEntities() {
        val s = visibleFocused()
        assertTrue(s.requestEntityCreation())
        s.onActivityPaused(); s.onContainerHidden()
        s.onActivityResumed(); s.onContainerShown()
        assertFalse(s.requestEntityCreation())
        s.onContainerClosed()
        assertTrue(s.requestEntityCreation(), "a closed container builds its entities again")
    }
}
