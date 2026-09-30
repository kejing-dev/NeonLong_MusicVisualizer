package com.technomancer.neonlong.ecs.systems

import com.pico.spatial.core.ecs.SceneUpdateContext
import com.pico.spatial.core.ecs.System
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.math.Quat
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.containers.PH_CUBE
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Phase 0 only: yaws the placeholder cube at 20°/s. Removed when DragonSystem arrives in Phase 1. */
class PhCubeSystem : System() {
    private var yawRad = 0f

    override fun update(context: SceneUpdateContext) {
        val cube = ServiceRegistry.entities[PH_CUBE] ?: return
        val transform = cube.components[TransformComponent::class.java] ?: return
        val dt = context.deltaTime.coerceIn(0f, 0.05f)
        yawRad = ((yawRad + YAW_RATE_RAD_PER_S * dt) % TWO_PI)
        val half = yawRad * 0.5f
        transform.setQuaternion(Quat(0f, sin(half), 0f, cos(half)))
    }

    private companion object {
        const val YAW_RATE_RAD_PER_S = (20.0 * PI / 180.0).toFloat()
        const val TWO_PI = (2.0 * PI).toFloat()
    }
}
