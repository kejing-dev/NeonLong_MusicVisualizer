package com.technomancer.neonlong.ecs.components

import com.pico.spatial.core.ecs.Component

/** Marks a body segment entity as touchable and remembers which segment it is. */
class SegmentTouchComponent(val segment: Int = 0) : Component()
