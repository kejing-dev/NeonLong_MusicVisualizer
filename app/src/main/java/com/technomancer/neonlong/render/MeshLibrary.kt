package com.technomancer.neonlong.render

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.pico.spatial.core.ecs.Entity
import com.pico.spatial.core.ecs.ModelComponent
import com.pico.spatial.core.ecs.ModelFormat
import com.pico.spatial.core.ecs.resource.MeshResource
import com.pico.spatial.core.ecs.LoadType
import com.pico.spatial.core.ecs.resource.TextureCreateOption
import com.pico.spatial.core.ecs.resource.TextureMipmapMode
import com.pico.spatial.core.ecs.resource.TextureResource
import com.pico.spatial.core.math.Vector3
import com.technomancer.neonlong.app.LOG_TAG
import com.technomancer.neonlong.logic.dragon.HeadZones
import kotlinx.coroutines.yield

/**
 * The generated models and textures, loaded once from the app's assets. A model whose file
 * is missing or won't load falls back to a code-built placeholder so the dragon still shows.
 */
class MeshLibrary private constructor(
    val block: MeshResource,
    val tower: MeshResource,
    val cylBase: MeshResource,
    val ball: MeshResource,
    val squareRing: MeshResource,
    /** One head model per color zone. */
    val headZones: List<MeshResource>,
    val jaw: MeshResource,
    val antlers: MeshResource,
    val glyphPlate: MeshResource,
    val blade: MeshResource,
    val limb: MeshResource,
    val talon: MeshResource,
    val torus: MeshResource,
    val pearlCore: MeshResource,
    val pearlShell: MeshResource,
    val pearlRing: MeshResource,
    /** Three crossed cards for soft glow halos. */
    val haloCards: MeshResource,
    val facade: TextureResource?,
    /** Emissive skin for body and head blocks. */
    val bodySkin: TextureResource?,
    /** Soft round falloff for additive halos. */
    val halo: TextureResource?,
    val armour: TextureResource?,
    val glyph: TextureResource?,
    val nightEnvironment: TextureResource?,
) {
    companion object {
        /**
         * Loads everything. The SDK requires the main thread for model loading, so this yields
         * after each file to keep the app responsive while it loads.
         */
        suspend fun load(context: Context): MeshLibrary {
            val start = System.nanoTime()
            val lib = MeshLibrary(
                block = step { mesh(context, "GEN_Block") { MeshResource.createBox(Vector3(1f, 1f, 1f), 0f) } },
                tower = step { mesh(context, "GEN_Tower3Tier") { MeshResource.createBox(Vector3(1f, 1f, 1f), 0f) } },
                cylBase = step { mesh(context, "GEN_CylBase") { MeshResource.createBox(Vector3(1f, 1f, 1f), 0f) } },
                ball = step { mesh(context, "GEN_Ball") { MeshResource.createSphere(1f) } },
                squareRing = step { mesh(context, "GEN_SquareRing") { MeshResource.createBox(Vector3(2f, 2f, 0.06f), 0f) } },
                headZones = List(HeadZones.COUNT) { z ->
                    // Placeholder if missing: one head-sized box, the other zones tiny.
                    val size = if (z == 0) Vector3(2.2f, 1.4f, 4f) else Vector3(0.01f, 0.01f, 0.01f)
                    step { mesh(context, "GEN_Head_$z") { MeshResource.createBox(size, 0f) } }
                },
                jaw = step { mesh(context, "GEN_Jaw") { MeshResource.createBox(Vector3(1.9f, 0.4f, 2.2f), 0f) } },
                antlers = step { mesh(context, "GEN_Antlers") { MeshResource.createBox(Vector3(0.1f, 0.1f, 0.1f), 0f) } },
                glyphPlate = step { mesh(context, "GEN_GlyphPlate") { MeshResource.createBox(Vector3(1f, 1f, 0.01f), 0f) } },
                blade = step { mesh(context, "GEN_Blade") { MeshResource.createBox(Vector3(1f, 1f, 1f), 0f) } },
                limb = step { mesh(context, "GEN_Limb") { MeshResource.createBox(Vector3(1f, 1f, 1f), 0f) } },
                talon = step { mesh(context, "GEN_Talon") { MeshResource.createBox(Vector3(1f, 1f, 1f), 0f) } },
                torus = step { mesh(context, "GEN_Torus") { MeshResource.createBox(Vector3(2f, 2f, 0.2f), 0f) } },
                pearlCore = step { mesh(context, "GEN_PearlCore") { MeshResource.createSphere(0.5f) } },
                pearlShell = step { mesh(context, "GEN_PearlShell") { MeshResource.createSphere(0.72f) } },
                pearlRing = step { mesh(context, "GEN_PearlRing") { MeshResource.createBox(Vector3(2f, 2f, 0.04f), 0f) } },
                haloCards = step { mesh(context, "GEN_HaloQuad") { MeshResource.createBox(Vector3(0.01f, 0.01f, 0.01f), 0f) } },
                facade = step { texture(context, "GEN_Facade") },
                bodySkin = step { texture(context, "GEN_BodySkin") },
                halo = step { texture(context, "GEN_Halo") },
                armour = step { texture(context, "GEN_Armour") },
                glyph = step { texture(context, "GEN_Glyph_Long") },
                nightEnvironment = step { texture(context, "GEN_IBL_Night") },
            )
            Log.i(LOG_TAG, "[boot] meshes and textures loaded in ${(System.nanoTime() - start) / 1_000_000} ms")
            return lib
        }

        /** Runs [block], then lets the main thread handle other work before the next load. */
        private suspend inline fun <T> step(block: () -> T): T = block().also { yield() }

        private fun mesh(context: Context, name: String, placeholder: () -> MeshResource): MeshResource = try {
            val entity = context.assets.openFd("models/$name.glb").use { fd -> Entity.load(fd, ModelFormat.GLTF) }
            val found = findMesh(entity) ?: error("no mesh inside")
            // The loaded model entity is never added to the scene; make its mesh outlive it.
            found.toGlobal()
            Log.i(LOG_TAG, "[boot] model $name loaded")
            found
        } catch (e: Throwable) {
            Log.w(LOG_TAG, "[boot] model $name failed (${e.javaClass.simpleName}: ${e.message}); using a placeholder")
            placeholder()
        }

        /** First mesh found in [entity] or its descendants. */
        private fun findMesh(entity: Entity): MeshResource? {
            entity.components[ModelComponent::class.java]?.mesh?.let { m -> return m }
            for (child in entity.getChildren()) findMesh(child)?.let { return it }
            return null
        }

        /** Loads a texture with mipmaps (smooth at a distance); falls back to a plain bitmap upload. */
        private fun texture(context: Context, name: String): TextureResource? {
            val path = "textures/$name.png"
            try {
                val option = TextureCreateOption().apply { mipmapMode = TextureMipmapMode.GENERATE_ALL }
                val t = TextureResource.load(path, LoadType.FROM_ASSETS, option).also { it.toGlobal() }
                Log.i(LOG_TAG, "[boot] texture $name loaded with mipmaps")
                return t
            } catch (e: Throwable) {
                Log.w(LOG_TAG, "[boot] texture $name mipmapped load failed (${e.message}); uploading the bitmap")
            }
            return try {
                val bmp = context.assets.open(path).use { BitmapFactory.decodeStream(it) }
                TextureResource.create(bmp).also { it.toGlobal() }
            } catch (e: Throwable) {
                Log.w(LOG_TAG, "[boot] texture $name failed: ${e.message}")
                null
            }
        }
    }
}
