package com.technomancer.neonlong.app

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Which windows are open and what the panel shows briefly, shared by the containers. */
class UiState {
    /** True while the control panel's content is on screen. */
    @Volatile
    var controlsOpen = false

    /** True once the control panel has been opened this session (stops the badge pulse). */
    @Volatile
    var panelOpenedOnce = false

    /** True while the safety notice is on screen. */
    @Volatile
    var safetyOpen = false

    private val toastMutable = MutableStateFlow<Toast?>(null)

    /** The toast to show, or null. */
    val toast: StateFlow<Toast?> = toastMutable

    /** A short message; [serial] makes repeats of the same message show again. */
    data class Toast(@StringRes val text: Int, val serial: Long)

    private var serial = 0L

    fun showToast(@StringRes text: Int) {
        serial++
        toastMutable.value = Toast(text, serial)
    }

    fun clearToast(shown: Toast) {
        if (toastMutable.value == shown) toastMutable.value = null
    }
}
