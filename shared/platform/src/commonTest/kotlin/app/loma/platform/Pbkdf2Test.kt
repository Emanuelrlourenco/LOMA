// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class Pbkdf2Test {
    private fun hex(b: ByteArray) = b.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    @Test
    fun vetorDaRfc7914() {
        assertEquals(
            "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783",
            hex(pbkdf2Sha256("passwd".encodeToByteArray(), "salt".encodeToByteArray(), iterations = 1, keyLength = 64)),
        )
    }

    @Test
    fun vetorConhecidoDe32Bytes() {
        assertEquals(
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
            hex(pbkdf2Sha256("password".encodeToByteArray(), "salt".encodeToByteArray(), iterations = 1)),
        )
    }

    @Test
    fun bytesAleatorios() {
        val a = secureRandomBytes(16)
        val b = secureRandomBytes(16)
        assertEquals(16, a.size)
        assertFalse(a.contentEquals(b))
    }
}
