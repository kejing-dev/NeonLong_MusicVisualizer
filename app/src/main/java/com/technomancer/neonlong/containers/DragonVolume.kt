package com.technomancer.neonlong.containers

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pico.spatial.core.container.SpatialContainerStateEvent
import com.pico.spatial.core.container.SpatialContainerStateObserver
import com.pico.spatial.core.container.SpatialContainerStateOwner
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.dsl.registerSystem
import com.pico.spatial.ui.foundation.dsl.unregisterSystem
import com.pico.spatial.ui.foundation.gesture.detectSpatialTapGesture
import com.pico.spatial.ui.platform.LengthUnit
import com.pico.spatial.ui.platform.LocalHandControllerHaptic
import com.pico.spatial.ui.platform.LocalPhysicalLengthConverter
import com.pico.spatial.ui.platform.LocalSpatialContainerStateOwner
import com.technomancer.neonlong.BuildConfig
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.debug.DebugFlags
import com.technomancer.neonlong.debug.EntitySpike
import com.technomancer.neonlong.ecs.systems.DragonSystem
import com.technomancer.neonlong.ecs.systems.InputSnapshotSystem

/** Container id of the dragon volume (also set in the manifest for the launch container). */
const val DRAGON_CONTAINER = "dragon"

/**
 * Content of the `dragon` Volumetric WindowContainer: the gray-box dragon flying its idle
 * loop, plus the turntable ring at the base. Systems are registered here, in frame order,
 * and unregistered when the container's content is disposed.
 */
@Composable
fun DragonVolume() {
    val reg = ServiceRegistry
    val context = LocalContext.current
    if (BuildConfig.DEBUG && !reg.spikeEnabled && DebugFlags.intentFlag(context, DebugFlags.EXTRA_SPIKE)) {
        reg.spikeEnabled = true
    }
    val spike = reg.spikeEnabled
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateOwner = LocalSpatialContainerStateOwner.current
    val lengths = LocalPhysicalLengthConverter.current
    val haptic = LocalHandControllerHaptic.current

    DisposableEffect(Unit) {
        registerSystem<InputSnapshotSystem>()
        registerSystem<DragonSystem>()
        if (spike) registerSystem<EntitySpike>()

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> reg.session.onActivityResumed()
                Lifecycle.Event.ON_PAUSE -> reg.session.onActivityPaused()
                else -> Unit
            }
            Log.i(LOG_TAG, "[boot] $DRAGON_CONTAINER lifecycle $event -> ${reg.session.visibility}")
        }
        val stateObserver = object : SpatialContainerStateObserver {
            override fun onStateChanged(source: SpatialContainerStateOwner, event: SpatialContainerStateEvent) {
                when (event) {
                    SpatialContainerStateEvent.ON_STAGED -> reg.session.onContainerShown()
                    SpatialContainerStateEvent.ON_UNSTAGED -> reg.session.onContainerHidden()
                    SpatialContainerStateEvent.ON_FOCUSED -> reg.session.onContainerFocused()
                    SpatialContainerStateEvent.ON_UNFOCUSED -> reg.session.onContainerUnfocused()
                    else -> Unit
                }
                Log.i(LOG_TAG, "[boot] $DRAGON_CONTAINER state $event -> ${reg.session.visibility}")
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        stateOwner.stateObservable.addObserver(stateObserver)
        // The container is on screen when its content is composed; state events refine this.
        reg.session.onContainerShown()
        reg.session.onContainerFocused()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            stateOwner.stateObservable.removeObserver(stateObserver)
            if (spike) unregisterSystem<EntitySpike>()
            unregisterSystem<DragonSystem>()
            unregisterSystem<InputSnapshotSystem>()
            reg.dragonEntities.destroy(reg.entities)
            EntitySpike.destroyAll()
            reg.session.onContainerClosed()
            Log.i(LOG_TAG, "[boot] $DRAGON_CONTAINER closed")
        }
    }

    SpatialView(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                val widthDp = with(lengths) { size.width.toDp() }
                val meters = lengths.dpToLength(widthDp, LengthUnit.Meters)
                if (meters > 0f) reg.volumeWidthMeters = meters
                Log.i(LOG_TAG, "[boot] $DRAGON_CONTAINER size ${size.width}x${size.height} px = $meters m wide")
            }
            .pointerInput(Unit) {
                if (spike) {
                    detectSpatialTapGesture(context) { tap ->
                        EntitySpike.onTap(tap.targetEntity, haptic)
                    }
                }
            },
        initial = { content, _ ->
            if (!reg.session.requestEntityCreation()) return@SpatialView
            if (spike) {
                EntitySpike.attach(content)
            } else {
                reg.dragonEntities.build(reg.sceneConfig, reg.dragonConfig, reg.entities)
                    .forEach { content.addEntity(it) }
            }
        },
    )
}
