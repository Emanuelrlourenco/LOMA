// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.intro

import androidx.compose.runtime.Composable

/** Toca o som "Brilho lento" da abertura, respeitando o modo silencioso da plataforma. */
expect class IntroSound {
    /** Toca o áudio já lido dos recursos; não faz nada se o aparelho estiver em silêncio. */
    fun play(bytes: ByteArray)
    fun stop()
}

/** Arquivo de som da plataforma dentro de composeResources (Android: .ogg; iOS: .m4a). */
internal expect val introSoundFile: String

@Composable
internal expect fun rememberIntroSound(): IntroSound

/** "Remover animações" do sistema ligado. */
@Composable
internal expect fun rememberReducedMotion(): Boolean
