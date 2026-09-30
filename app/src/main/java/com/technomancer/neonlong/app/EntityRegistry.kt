package com.technomancer.neonlong.app

import com.pico.spatial.core.ecs.Entity

/**
 * ID → Entity map filled at creation. Systems look entities up here instead of
 * searching by name each frame.
 */
class EntityRegistry {
    private val byKey = HashMap<String, Entity>()

    fun put(key: String, entity: Entity) {
        byKey[key] = entity
    }

    operator fun get(key: String): Entity? = byKey[key]

    fun remove(key: String): Entity? = byKey.remove(key)
}
