// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.salon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneBrTest {
    @Test
    fun normaliza() {
        listOf("(11) 98765-4321", "11987654321", "+55 11 98765-4321", "011 98765 4321", "55 11 98765-4321")
            .forEach { assertEquals("5511987654321", PhoneBr.normalize(it), it) }
        assertEquals("551134567890", PhoneBr.normalize("(11) 3456-7890"))
        assertEquals("351912345678", PhoneBr.normalize("+351 912 345 678"))
        listOf("abc", "12345", "", "   ").forEach { assertNull(PhoneBr.normalize(it), it) }
    }

    @Test
    fun formata() {
        assertEquals("(11) 98765-4321", PhoneBr.format("5511987654321"))
        assertEquals("(11) 3456-7890", PhoneBr.format("551134567890"))
        assertEquals("+351912345678", PhoneBr.format("351912345678"))
    }
}
