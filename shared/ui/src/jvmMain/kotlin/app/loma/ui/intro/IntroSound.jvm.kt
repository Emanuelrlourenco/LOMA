// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** No desktop (só testes) a abertura é muda. */
actual class IntroSound {
    actual fun play(bytes: ByteArray) = Unit
    actual fun stop() = Unit
}

internal actual val introSoundFile: String = "files/loma-abertura-brilho-lento.ogg"

@Composable
internal actual fun rememberIntroSound(): IntroSound = remember { IntroSound() }

@Composable
internal actual fun rememberReducedMotion(): Boolean = false
