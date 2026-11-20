// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.intro

import app.loma.ui.TestGraph
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.loma.ui.shell.LomaApp
import io.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LomaIntroScreenshotTest {

    @BeforeTest
    fun idiomaPortugues() {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
    }

    @Test
    fun quadrosDaAbertura() = runDesktopComposeUiTest(width = 390, height = 844) {
        mainClock.autoAdvance = false
        setContent { LomaApp(graph = TestGraph().graph, darkTheme = false, soundEnabled = false) }
        var agora = 0L
        listOf(1_000L, 2_200L, 3_400L, 4_300L, 4_600L).forEach { ms ->
            mainClock.advanceTimeBy(ms - agora)
            agora = ms
            onRoot().captureRoboImage("screenshots/abertura-${ms}ms.png")
        }
    }
}
