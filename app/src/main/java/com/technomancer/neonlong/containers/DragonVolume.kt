package com.technomancer.neonlong.containers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.TransformComponent
import com.pico.spatial.core.ecs.resource.BlendingMode
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.resource.PhysicallyBasedMaterial
import com.pico.spatial.core.math.Color4
import com.pico.spatial.core.math.Vector3
import com.pico.spatial.ui.foundation.content.SpatialView
import com.pico.spatial.ui.foundation.dsl.registerSystem
import com.pico.spatial.ui.foundation.dsl.unregisterSystem
import com.technomancer.neonlong.app.ServiceRegistry
import com.technomancer.neonlong.ecs.systems.PhCubeSystem

/** Registry key for the Phase 0 placeholder cube. */
const val PH_CUBE = "PH_Cube"

/**
 * Content of the `dragon` Volumetric WindowContainer. Phase 0: one 0.2 m placeholder cube that
 * [PhCubeSystem] spins at 20°/s about Y. Replaced by the gray-box dragon in Phase 1.
 */
@Composable
fun DragonVolume() {
    DisposableEffect(Unit) {
        registerSystem<PhCubeSystem>()
        onDispose {
            unregisterSystem<PhCubeSystem>()
            ServiceRegistry.entities.remove(PH_CUBE)
        }
    }
    SpatialView(
        modifier = Modifier,
        initial = { content, _ ->
            val material = PhysicallyBasedMaterial.create(BlendingMode.OPAQUE).apply {
                setBaseColor(Color4(0.10f, 0.09f, 0.16f, 1f))
                setEmissiveColor(Color4(1.0f, 0.18f, 0.53f, 1f)) // Kowloon stop0 #ff2e88
                setMetallic(0.75f)
                setRoughness(0.33f)
            }
            val cube = Entity().apply {
                setName(PH_CUBE)
                components.set(ModelComponent(MeshResource.createBox(Vector3(0.2f, 0.2f, 0.2f), 0f), material))
                components.set(TransformComponent())
            }
            ServiceRegistry.entities.put(PH_CUBE, cube)
            content.addEntity(cube)
        },
    )
}
