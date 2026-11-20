// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.intro

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File

actual class IntroSound(private val context: Context) {
    private var pool: SoundPool? = null
    private var streamId = 0

    actual fun play(bytes: ByteArray) {
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        runCatching {
            val file = File(context.cacheDir, "loma-abertura.ogg")
            if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val created = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attributes).build()
            pool = created
            created.setOnLoadCompleteListener { loaded, sampleId, status ->
                if (status == 0 && pool === loaded) streamId = loaded.play(sampleId, 0.9f, 0.9f, 1, 0, 1f)
            }
            created.load(file.absolutePath, 1)
        }
    }

    actual fun stop() {
        pool?.let {
            if (streamId != 0) it.stop(streamId)
            it.release()
        }
        pool = null
        streamId = 0
    }
}

internal actual val introSoundFile: String = "files/loma-abertura-brilho-lento.ogg"

@Composable
internal actual fun rememberIntroSound(): IntroSound {
    val context = LocalContext.current.applicationContext
    return remember(context) { IntroSound(context) }
}

@Composable
internal actual fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
