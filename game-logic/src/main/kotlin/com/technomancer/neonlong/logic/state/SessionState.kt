package com.technomancer.neonlong.logic.state

/**
 * Tracks whether the dragon container is visible and focused, and whether the simulation
 * should run. Fed from the Activity lifecycle and the container's state events.
 */
class SessionState {
    enum class Visibility { VISIBLE_FOCUSED, VISIBLE_UNFOCUSED, HIDDEN }

    var activityResumed = false
        private set
    var containerShown = false
        private set
    var containerFocused = false
        private set
    var headsetWorn = true
        private set

    /** True once the container's entities exist; cleared only when the container closes. */
    var entitiesCreated = false
        private set

    val visibility: Visibility
        get() = when {
            !activityResumed || !containerShown || !headsetWorn -> Visibility.HIDDEN
            containerFocused -> Visibility.VISIBLE_FOCUSED
            else -> Visibility.VISIBLE_UNFOCUSED
        }

    /** The simulation advances only while the dragon can be seen (focused or not). */
    val running: Boolean
        get() = visibility != Visibility.HIDDEN

    fun onActivityResumed() { activityResumed = true }
    fun onActivityPaused() { activityResumed = false }
    fun onContainerShown() { containerShown = true }
    fun onContainerHidden() { containerShown = false }
    fun onContainerFocused() { containerFocused = true }
    fun onContainerUnfocused() { containerFocused = false }
    fun onHeadsetWorn(worn: Boolean) { headsetWorn = worn }

    /**
     * Called when the container's content is built. Returns true if entities must be created,
     * false if they already exist (a resume never creates duplicates).
     */
    fun requestEntityCreation(): Boolean {
        if (entitiesCreated) return false
        entitiesCreated = true
        return true
    }

    /** The container closed and its entities were destroyed. */
    fun onContainerClosed() {
        containerShown = false
        containerFocused = false
        entitiesCreated = false
    }
}
