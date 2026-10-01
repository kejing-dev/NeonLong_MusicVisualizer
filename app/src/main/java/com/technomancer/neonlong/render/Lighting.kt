package com.technomancer.neonlong.render

import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ImageBasedLightComponent
import com.pico.spatial.core.ecs.ImageBasedLightReceiverComponent
import com.pico.spatial.core.ecs.ImageBasedLightSource
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.TextureResource
import com.technomancer.neonlong.app.LOG_TAG

/**
 * Night lighting for the dragon's metal: an image-based light from the generated night sky
 * (dark blue-violet, with a soft magenta key toward +X). Metal parts receive it.
 */
object Lighting {
    const val NAME = "IBL_Night"

    fun create(environment: TextureResource?): Entity {
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
        } else {
            Log.w(LOG_TAG, "[boot] no night environment texture; metal uses the system lighting")
        }
        return rig
    }

    /** Lets a part reflect the rig's environment light. */
    fun receiver(rig: Entity) = ImageBasedLightReceiverComponent(rig)
}
