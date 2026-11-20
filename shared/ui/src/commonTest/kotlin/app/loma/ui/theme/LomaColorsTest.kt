// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LomaColorsTest {

    @Test
    fun tokensClarosSaoOsDasTelasAprovadas() {
        assertEquals(Color(0xFFF3F2FA), LomaPalette.Background)
        assertEquals(Color(0xFFFFFFFF), LomaPalette.Surface)
        assertEquals(Color(0xFFE7E4F8), LomaPalette.Container)
        assertEquals(Color(0xFF17165C), LomaPalette.Navy)
        assertEquals(Color(0xFF2A2985), LomaPalette.NavyLight)
        assertEquals(Color(0xFF5ED6B4), LomaPalette.Mint)
        assertEquals(Color(0xFF0B3A2E), LomaPalette.OnMint)
        assertEquals(Color(0xFFE3F8F1), LomaPalette.MintSoft)
        assertEquals(Color(0xFFB69CF2), LomaPalette.Lilac)
        assertEquals(Color(0xFFFF7A66), LomaPalette.Coral)
        assertEquals(Color(0xFFFFE2DC), LomaPalette.AlertSoft)
        assertEquals(Color(0xFF7A1A0B), LomaPalette.OnAlertSoft)
        assertEquals(Color(0xFF5B5A7A), LomaPalette.TextSecondary)
        assertEquals(Color(0xFFC9C6E6), LomaPalette.Border)
    }

    @Test
    fun contrasteMaximoEhVinteEUm() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
    }

    @Test
    fun textosTemContrasteSuficienteNoClaroENoEscuro() {
        for (colors in listOf(LightLomaColors, DarkLomaColors)) {
            assertLegivel(colors.textPrimary, colors.background, "texto principal")
            assertLegivel(colors.textSecondary, colors.background, "texto secundário")
            assertLegivel(colors.navIndicatorIcon, colors.navIndicator, "ícone da aba selecionada")
        }
        assertLegivel(Color.White, LomaPalette.Navy, "branco sobre marinho")
        assertLegivel(LomaPalette.OnMint, LomaPalette.Mint, "texto sobre menta")
        assertLegivel(LomaPalette.Navy, LomaPalette.Lilac, "marinho sobre lilás")
        assertLegivel(LomaPalette.OnAlertSoft, LomaPalette.AlertSoft, "texto de alerta")
    }

    private fun assertLegivel(texto: Color, fundo: Color, nome: String) {
        val ratio = contrastRatio(texto, fundo)
        assertTrue(ratio >= 4.5, "$nome com contraste $ratio (mínimo 4,5)")
    }
}
