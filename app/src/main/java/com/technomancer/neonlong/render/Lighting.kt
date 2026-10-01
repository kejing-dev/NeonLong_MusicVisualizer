package com.technomancer.neonlong.render

import android.util.Log
import com.pico.spatial.core.ecs.DirectionalLightComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ImageBasedLightComponent
import com.pico.spatial.core.ecs.ImageBasedLightReceiverComponent
import com.pico.spatial.core.ecs.ImageBasedLightSource
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Color4
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.app.SdkMath
import kotlin.math.cos
import kotlin.math.sin

/**
 * Temporary night lighting for the dragon's metal until the Spatial Editor environment exists:
 * an image-based light from the generated night sky, plus a soft magenta key and a cool fill
 * so the metal has highlights to show.
 */
object Lighting {
    const val NAME = "PH_NightLight"

    /** Builds the light rig. Parts that should reflect the environment get [receiver]. */
    fun create(environment: com.pico.spatial.core.ecs.resource.TextureResource?): Entity {
        val rig = Entity().apply {
            setName(NAME)
            components.set(TransformComponent())
        }
        if (environment != null) {
            try {
                rig.components.set(ImageBasedLightComponent(ImageBasedLightSource.Single(environment), 0f))
                Log.i(LOG_TAG, "[boot] night environment light on")
            } catch (e: Throwable) {
                Log.w(LOG_TAG, "[boot] environment light failed: ${e.message}")
            }
        }
        rig.addChild(directional("PH_KeyLight", Color4(1.0f, 0.18f, 0.53f, 1f), 0.6f, yawDeg = 90f, pitchDeg = -30f))
        rig.addChild(directional("PH_FillLight", Color4(0.45f, 0.40f, 1.0f, 1f), 0.35f, yawDeg = -35f, pitchDeg = -55f))
        return rig
    }

    /** Lets [part] reflect the rig's environment light. */
    fun receiver(rig: Entity) = ImageBasedLightReceiverComponent(rig)

    private fun directional(name: String, color: Color4, intensity: Float, yawDeg: Float, pitchDeg: Float): Entity {
        val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
        val pitch = Math.toRadians(pitchDeg.toDouble()).toFloat()
        // Yaw about Y, then pitch about X.
        val cy = cos(yaw / 2); val sy = sin(yaw / 2)
        val cp = cos(pitch / 2); val sp = sin(pitch / 2)
        return Entity().apply {
            setName(name)
            components.set(TransformComponent().apply { setQuaternion(SdkMath.quat(cy * sp, sy * cp, -sy * sp, cy * cp)) })
            components.set(DirectionalLightComponent(color, intensity))
        }
    }
}
