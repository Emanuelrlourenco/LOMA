// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

/** Fotos das clientes, guardadas no armazenamento privado do app. */
interface PhotoStore {
    /** Copia a imagem escolhida (reduzida) e devolve o nome do arquivo, ou `null` se não deu. */
    suspend fun importPhoto(sourceUri: String): String?
    suspend fun read(file: String): ByteArray?
    suspend fun delete(file: String)
}
