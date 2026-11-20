// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.intro

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.loma.ui.shell.LomaApp
import app.loma.ui.theme.LomaTheme
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class LomaIntroTest {

    @BeforeTest
    fun idiomaPortugues() {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
    }

    @Test
    fun terminaSozinhaEmTotalMs() = runComposeUiTest {
        mainClock.autoAdvance = false
        var chamadas = 0
        setContent { LomaTheme(darkTheme = false) { LomaIntro(soundEnabled = false, reducedMotion = false, onFinished = { chamadas++ }) } }

        mainClock.advanceTimeBy(IntroTimeline.TOTAL_MS - 200L)
        assertEquals(0, chamadas)
        mainClock.advanceTimeBy(400)
        assertEquals(1, chamadas)
        mainClock.advanceTimeBy(5_000)
        assertEquals(1, chamadas)
    }

    @Test
    fun toquePulaNaHoraENaoTerminaDeNovo() = runComposeUiTest {
        mainClock.autoAdvance = false
        var chamadas = 0
        setContent { LomaTheme(darkTheme = false) { LomaIntro(soundEnabled = false, reducedMotion = false, onFinished = { chamadas++ }) } }

        mainClock.advanceTimeBy(1_000)
        onNodeWithTag("loma_intro").performClick()
        assertEquals(1, chamadas)
        mainClock.advanceTimeBy(IntroTimeline.TOTAL_MS.toLong())
        assertEquals(1, chamadas)
    }

    @Test
    fun semAnimacoesMostraONomePor600ms() = runComposeUiTest {
        mainClock.autoAdvance = false
        var chamadas = 0
        setContent { LomaTheme(darkTheme = false) { LomaIntro(soundEnabled = false, reducedMotion = true, onFinished = { chamadas++ }) } }

        mainClock.advanceTimeBy(IntroTimeline.REDUCED_MOTION_MS - 100L)
        assertEquals(0, chamadas)
        mainClock.advanceTimeBy(200)
        assertEquals(1, chamadas)
    }

    @Test
    fun aberturaNaoVoltaQuandoATelaERecriada() = runComposeUiTest {
        mainClock.autoAdvance = false
        var registry by mutableStateOf(SaveableStateRegistry(null) { true })
        var visivel by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                if (visivel) LomaApp(darkTheme = false)
            }
        }
        onNodeWithTag("loma_intro").assertExists()

        mainClock.advanceTimeBy(IntroTimeline.TOTAL_MS + 200L)
        onNodeWithTag("loma_intro").assertDoesNotExist()

        val salvo = runOnIdle { registry.performSave() }
        runOnIdle { visivel = false }
        runOnIdle {
            registry = SaveableStateRegistry(salvo) { true }
            visivel = true
        }
        mainClock.advanceTimeByFrame()

        onNodeWithTag("loma_intro").assertDoesNotExist()
        onNodeWithTag("tab_inicio").assertIsSelected()
    }
}
