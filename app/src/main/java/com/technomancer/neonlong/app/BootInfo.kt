package com.technomancer.neonlong.app

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Display
import com.technomancer.neonlong.BuildConfig
import com.technomancer.neonlong.logic.LOGIC_VERSION

/** Log tag shared by the whole app. */
const val LOG_TAG = "NeonLong"

/** Writes the one-line `[boot]` environment record the human pastes into docs/PROGRESS.md. */
object BootInfo {
    fun log(context: Context) {
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
        val refreshHz = display?.refreshRate ?: -1f
        val supportedHz = display?.supportedModes?.map { it.refreshRate }?.distinct()?.sorted()
        Log.i(
            LOG_TAG,
            "[boot] sdkInt=${Build.VERSION.SDK_INT} display=${Build.DISPLAY} model=${Build.MODEL} " +
                "refreshHz=$refreshHz supportedHz=$supportedHz spatialBom=${BuildConfig.SPATIAL_BOM_VERSION} " +
                "logic=$LOGIC_VERSION app=${BuildConfig.VERSION_NAME} debug=${BuildConfig.DEBUG}"
        )
    }
}
