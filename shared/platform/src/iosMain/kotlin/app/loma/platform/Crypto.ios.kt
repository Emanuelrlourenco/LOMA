// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCKeyDerivationPBKDF
import platform.CoreCrypto.kCCPBKDF2
import platform.CoreCrypto.kCCPRFHmacAlgSHA256
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault

@OptIn(ExperimentalForeignApi::class)
actual fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
    require(password.isNotEmpty() && salt.isNotEmpty()) { "senha e sal não podem ser vazios" }
    val out = ByteArray(keyLength)
    val status = password.usePinned { p ->
        salt.usePinned { s ->
            out.usePinned { o ->
                CCKeyDerivationPBKDF(
                    kCCPBKDF2.convert(),
                    p.addressOf(0), password.size.convert(),
                    s.addressOf(0).reinterpret(), salt.size.convert(),
                    kCCPRFHmacAlgSHA256.convert(), iterations.convert(),
                    o.addressOf(0).reinterpret(), keyLength.convert(),
                )
            }
        }
    }
    check(status == 0) { "CCKeyDerivationPBKDF falhou: $status" }
    return out
}

@OptIn(ExperimentalForeignApi::class)
actual fun secureRandomBytes(size: Int): ByteArray {
    val out = ByteArray(size)
    if (size == 0) return out
    val status = out.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0)) }
    check(status == 0) { "SecRandomCopyBytes falhou: $status" }
    return out
}
