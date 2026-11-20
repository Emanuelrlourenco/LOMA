// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** BiometricPrompt do AndroidX. A MainActivity se registra em [activity] (referência fraca). */
class AndroidDeviceAuth(context: Context) : DeviceAuth {
    private val app = context.applicationContext
    private var activityRef: WeakReference<FragmentActivity>? = null

    var activity: FragmentActivity?
        get() = activityRef?.get()
        set(value) { activityRef = value?.let(::WeakReference) }

    override fun biometricAvailable(): Boolean =
        BiometricManager.from(app).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

    override fun deviceSecure(): Boolean = app.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    // Chamado da tela (thread principal), como o BiometricPrompt exige.
    override suspend fun authenticate(kind: AuthKind, title: String, subtitle: String, cancel: String): Boolean {
        val act = activity ?: return false
        return suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                act,
                ContextCompat.getMainExecutor(act),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(true)
                    }

                    // Cancelou, sem cadastro, bloqueado por tentativas…: volta para o PIN.
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (cont.isActive) cont.resume(false)
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder().setTitle(title).apply {
                if (subtitle.isNotEmpty()) setSubtitle(subtitle)
                if (kind == AuthKind.BIOMETRIC) {
                    setAllowedAuthenticators(BIOMETRIC_WEAK)
                    setNegativeButtonText(cancel)
                } else {
                    // Fraca + credencial do aparelho funciona do Android 8 em diante.
                    setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                }
            }.build()
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(info)
        }
    }
}
