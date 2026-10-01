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
    val facade: TextureResource?,
    val nightEnvironment: TextureResource?,
) {
    companion object {
        /** Loads everything. The SDK requires the main thread for model loading. */
        fun load(context: Context): MeshLibrary {
            val start = System.nanoTime()
            val lib = MeshLibrary(
                block = mesh(context, "GEN_Block") { MeshResource.createBox(Vector3(1f, 1f, 1f), 0f) },
                tower = mesh(context, "GEN_Tower3Tier") { MeshResource.createBox(Vector3(1f, 1f, 1f), 0f) },
                cylBase = mesh(context, "GEN_CylBase") { MeshResource.createBox(Vector3(1f, 1f, 1f), 0f) },
                ball = mesh(context, "GEN_Ball") { MeshResource.createSphere(1f) },
                squareRing = mesh(context, "GEN_SquareRing") { MeshResource.createBox(Vector3(2f, 2f, 0.06f), 0f) },
                facade = texture(context, "GEN_Facade"),
                nightEnvironment = texture(context, "GEN_EnvNightTemp"),
            )
            Log.i(LOG_TAG, "[boot] meshes and textures loaded in ${(System.nanoTime() - start) / 1_000_000} ms")
            return lib
        }

        private fun mesh(context: Context, name: String, placeholder: () -> MeshResource): MeshResource = try {
            val entity = context.assets.openFd("models/$name.glb").use { fd -> Entity.load(fd, ModelFormat.GLTF) }
            val found = findMesh(entity) ?: error("no mesh inside")
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
                val t = TextureResource.load(path, LoadType.FROM_ASSETS, option)
                Log.i(LOG_TAG, "[boot] texture $name loaded with mipmaps")
                return t
            } catch (e: Throwable) {
                Log.w(LOG_TAG, "[boot] texture $name mipmapped load failed (${e.message}); uploading the bitmap")
            }
            return try {
                val bmp = context.assets.open(path).use { BitmapFactory.decodeStream(it) }
                TextureResource.create(bmp)
            } catch (e: Throwable) {
                Log.w(LOG_TAG, "[boot] texture $name failed: ${e.message}")
                null
            }
        }
    }
}
