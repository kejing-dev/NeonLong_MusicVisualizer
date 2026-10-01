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
import com.technomancer.neonlong.ecs.systems.FiberMeshSystem
import com.technomancer.neonlong.ecs.systems.InputSnapshotSystem
import com.technomancer.neonlong.ecs.systems.PerfSystem
import com.technomancer.neonlong.render.MeshLibrary
import com.technomancer.neonlong.input.ControllerAdapter
import com.technomancer.neonlong.input.HandTrackingAdapter
import com.technomancer.neonlong.input.Haptics
import com.technomancer.neonlong.input.InputEvents
import com.technomancer.neonlong.logic.view.TurntableMath
import com.technomancer.neonlong.render.Badge
import com.technomancer.neonlong.render.Turntable
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.ui.foundation.gesture.TargetEntity
import com.pico.spatial.ui.foundation.gesture.detectSpatialDragGesture
import com.pico.spatial.ui.geometry.Offset3D
import com.pico.spatial.ui.platform.PhysicalLengthConverter
import androidx.compose.runtime.remember

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
    val haptics = remember(haptic) { Haptics(haptic) }

    DisposableEffect(Unit) {
        registerSystem<InputSnapshotSystem>()
        registerSystem<DragonSystem>()
        registerSystem<FiberMeshSystem>()
        registerSystem<PerfSystem>()
        if (spike) registerSystem<EntitySpike>()
        reg.audio.start()

        val hands = HandTrackingAdapter().also { it.start() }
        val controllers = ControllerAdapter(reg.events).also { it.start() }
        reg.handTracking = hands

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
            unregisterSystem<PerfSystem>()
            unregisterSystem<FiberMeshSystem>()
            unregisterSystem<DragonSystem>()
            unregisterSystem<InputSnapshotSystem>()
            reg.audio.release()
            reg.handTracking = null
            hands.stop()
            controllers.stop()
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
            .pointerInput("tap") {
                detectSpatialTapGesture(context) { tap ->
                    if (spike) {
                        EntitySpike.onTap(tap.targetEntity, haptic)
                    } else {
                        onTap(tap.targetEntity, haptics)
                    }
                }
            }
            .pointerInput("turntable") {
                if (!spike) {
                    val dragger = TurntableDrag(lengths, haptics)
                    detectSpatialDragGesture(
                        context,
                        TargetEntity.any { it.getName() == Turntable.NAME },
                        { _, _ -> dragger.start() },
                        { _ -> dragger.end() },
                        { _ -> dragger.end() },
                    ) { drag -> dragger.move(drag.dragAmount) }
                }
            },
        initial = { content, _ ->
            if (!reg.session.requestEntityCreation()) return@SpatialView
            if (spike) {
                EntitySpike.attach(content)
            } else {
                // The SDK loads models on the main thread only.
                val library = MeshLibrary.load(context)
                reg.dragonEntities.build(reg.sceneConfig, library, reg.entities).forEach { content.addEntity(it) }
            }
        },
    )
}

/** Routes a tap to a segment ripple or the badge, with a haptic pulse. Runs on the UI thread. */
private fun onTap(target: Entity?, haptics: Haptics) {
    val reg = ServiceRegistry
    val segment = reg.dragonEntities.segmentIndexOf(target)
    when {
        segment >= 0 -> {
            reg.events.post(InputEvents.SEGMENT_TOUCH, segment)
            haptics.segment()
        }
        target != null && target.getName() == Badge.NAME -> {
            reg.events.post(InputEvents.BADGE)
            haptics.badge()
        }
    }
}

/**
 * Turns a drag on the turntable into dragon yaw: sliding the hand sideways along the front of
 * the ring turns the dragon by the same arc (gain 1), with a haptic tick every 15°.
 */
private class TurntableDrag(private val lengths: PhysicalLengthConverter, private val haptics: Haptics) {
    private var loggedUnits = false

    fun start() {
        Log.i(LOG_TAG, "[input] turntable drag start")
    }

    fun end() {
        Log.i(LOG_TAG, "[input] turntable yaw ${Math.toDegrees(ServiceRegistry.dragonYaw.toDouble()).toInt()}°")
    }

    fun move(amount: Offset3D) {
        val reg = ServiceRegistry
        // Drag amounts arrive in layout pixels; convert to meters.
        val dxDp = with(lengths) { amount.x.toDp() }
        val dx = lengths.dpToLength(dxDp, LengthUnit.Meters)
        if (!loggedUnits) {
            loggedUnits = true
            Log.i(LOG_TAG, "[input] first drag step: raw x=${amount.x} → $dx m")
        }
        val radius = reg.sceneConfig.turntableRadiusFrac * reg.sceneConfig.defaultWidth
        val before = reg.dragonYaw
        // Sliding right along the front turns the front to the right: clockwise from above.
        val after = TurntableMath.wrap(before - TurntableMath.yawFromSlide(dx, radius, reg.sceneConfig.turntableGain))
        reg.dragonYaw = after
        if (TurntableMath.ticksCrossed(before, after, 15f) > 0) haptics.turntableTick()
    }
}
