package com.technomancer.neonlong.audio

import android.util.Log
import com.pico.spatial.core.ecs.ChannelAudioComponent
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.LoadType
import com.pico.spatial.core.ecs.ObjectAudioComponent
import com.pico.spatial.core.ecs.resource.AudioResource
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.logic.config.UiConfig
import kotlin.math.pow

/**
 * Interface sounds. Taps on the badge, ring and segments sound from the badge in 3D; panel
 * controls play without position. Muted when the "Interface sounds" setting is off.
 * Call from the main thread.
 */
class UiSfx(private val config: UiConfig, private val enabled: () -> Boolean) {
    private var selectSound: AudioResource? = null
    private var hoverSound: AudioResource? = null
    private var badge: Entity? = null
    private var channel: Entity? = null

    /** Loads the sounds and puts the sound sources on [badgeEntity] and under [parent]. */
    fun attach(badgeEntity: Entity, parent: Entity) {
        val volume = 10f.pow(config.uiSfxGainDb / 20f)
        try {
            selectSound = AudioResource.load("GEN_ui_select", "asset://audio/GEN_ui_select.wav", LoadType.FROM_ASSETS).also { it.toGlobal() }
            hoverSound = AudioResource.load("GEN_ui_hover", "asset://audio/GEN_ui_hover.wav", LoadType.FROM_ASSETS).also { it.toGlobal() }
            badgeEntity.components.set(ObjectAudioComponent(volume = volume))
            badge = badgeEntity
            channel = Entity().apply {
                setName("PH_UiSfxChannel")
                components.set(ChannelAudioComponent(volume))
            }.also { parent.addChild(it) }
            Log.i(LOG_TAG, "[audio] interface sounds ready at ${config.uiSfxGainDb} dB")
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "[audio] interface sounds unavailable: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun detach() {
        badge = null
        channel = null
    }

    /** A 3D item (badge, ring, segment) was activated. */
    fun select3D() = play(badge, selectSound)

    /** A panel control was activated. */
    fun select() = play(channel, selectSound)

    /** A slider moved by one step. */
    fun tick() = play(channel, hoverSound)

    private fun play(source: Entity?, sound: AudioResource?) {
        if (source == null || sound == null || !enabled()) return
        try {
            source.playAudio(sound)
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "[audio] interface sound failed: ${e.message}")
        }
    }
}
