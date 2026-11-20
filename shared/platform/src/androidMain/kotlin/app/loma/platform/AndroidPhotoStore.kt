// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Guarda as fotos em filesDir/clientes, reduzidas para no máximo 512 px no maior lado (JPEG 85). */
class AndroidPhotoStore(private val context: Context) : PhotoStore {

    private val dir: File get() = File(context.filesDir, "clientes").apply { mkdirs() }

    override suspend fun importPhoto(sourceUri: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(Uri.parse(sourceUri)) ?: return@runCatching null
            val scaled = scale(bitmap, MAX_SIDE)
            val name = "${UUID.randomUUID()}.jpg"
            File(dir, name).outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            name
        }.getOrNull()
    }

    override suspend fun read(file: String): ByteArray? = withContext(Dispatchers.IO) {
        File(dir, file).takeIf { it.exists() }?.readBytes()
    }

    override suspend fun delete(file: String) {
        withContext(Dispatchers.IO) { File(dir, file).delete() }
    }

    private fun decode(uri: Uri): Bitmap? =
        if (Build.VERSION.SDK_INT >= 28) {
            // ImageDecoder já aplica a rotação da foto.
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > MAX_SIDE) {
                    val f = MAX_SIDE.toFloat() / largest
                    decoder.setTargetSize((info.size.width * f).toInt().coerceAtLeast(1), (info.size.height * f).toInt().coerceAtLeast(1))
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        }

    private fun scale(b: Bitmap, max: Int): Bitmap {
        val largest = maxOf(b.width, b.height)
        if (largest <= max) return b
        val f = max.toFloat() / largest
        return Bitmap.createScaledBitmap(b, (b.width * f).toInt().coerceAtLeast(1), (b.height * f).toInt().coerceAtLeast(1), true)
    }

    private companion object {
        const val MAX_SIDE = 512
    }
}
