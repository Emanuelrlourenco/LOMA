// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import io.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LomaShellScreenshotTest {

    @BeforeTest
    fun idiomaPortugues() {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"))
    }

    @Test
    fun shellClaro() = runDesktopComposeUiTest(width = 390, height = 844) {
        setContent { LomaApp(darkTheme = false) }
        onRoot().captureRoboImage("screenshots/shell-claro.png")
    }

    @Test
    fun shellEscuro() = runDesktopComposeUiTest(width = 390, height = 844) {
        setContent { LomaApp(darkTheme = true) }
        onRoot().captureRoboImage("screenshots/shell-escuro.png")
    }
}
