// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics

/** LocalAuthentication: Face ID/Touch ID e o código do iPhone. */
@OptIn(ExperimentalForeignApi::class)
class IosDeviceAuth : DeviceAuth {
    override fun biometricAvailable(): Boolean =
        LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)

    override fun deviceSecure(): Boolean = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)

    override suspend fun authenticate(kind: AuthKind, title: String, subtitle: String, cancel: String): Boolean =
        suspendCancellableCoroutine { cont ->
            val context = LAContext()
            context.localizedCancelTitle = cancel
            // Na digital, sem o botão "Digitar senha": a alternativa é o PIN do LOMA.
            if (kind == AuthKind.BIOMETRIC) context.localizedFallbackTitle = ""
            val policy = if (kind == AuthKind.BIOMETRIC) LAPolicyDeviceOwnerAuthenticationWithBiometrics else LAPolicyDeviceOwnerAuthentication
            cont.invokeOnCancellation { context.invalidate() }
            context.evaluatePolicy(policy, localizedReason = subtitle.ifEmpty { title }) { ok, _ ->
                if (cont.isActive) cont.resume(ok)
            }
        }
}
