// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class IntroSound {
    private var player: AVAudioPlayer? = null

    actual fun play(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        // Categoria "ambient": mistura com outros sons e respeita a chave de silencioso.
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryAmbient, null)
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        player = AVAudioPlayer(data = data, error = null).also {
            it.volume = 0.9f
            it.prepareToPlay()
            it.play()
        }
    }

    actual fun stop() {
        player?.stop()
        player = null
    }
}

internal actual val introSoundFile: String = "files/loma-abertura-brilho-lento.m4a"

@Composable
internal actual fun rememberIntroSound(): IntroSound = remember { IntroSound() }

@Composable
internal actual fun rememberReducedMotion(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
