// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/** Fotos no Application Support (fora do backup do iCloud). O seletor de fotos do iOS chega na fase 7. */
@OptIn(ExperimentalForeignApi::class)
class IosPhotoStore : PhotoStore {

    private fun path(file: String): String {
        val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
        return "$base/clientes/$file"
    }

    override suspend fun importPhoto(sourceUri: String): String? = null

    override suspend fun read(file: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile(path(file)) ?: return null
        val size = data.length.toInt()
        if (size == 0) return ByteArray(0)
        return ByteArray(size).apply { usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
    }

    override suspend fun delete(file: String) {
        NSFileManager.defaultManager.removeItemAtPath(path(file), null)
    }
}
