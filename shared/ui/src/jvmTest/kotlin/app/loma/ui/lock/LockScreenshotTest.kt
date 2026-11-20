// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.lock

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import app.loma.data.SettingKeys
import app.loma.ui.TestGraph
import app.loma.ui.TestHost
import app.loma.ui.settings.SecurityScreen
import app.loma.ui.shell.LomaApp
import io.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.runBlocking

/** Capturas para conferir com a tela aprovada T-Bloqueio (Carla, 2 dígitos digitados). */
@OptIn(ExperimentalTestApi::class)
class LockScreenshotTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    @Test
    fun bloqueio() = runDesktopComposeUiTest(width = 390, height = 844) {
        val g = TestGraph(pin = "1234")
        g.lock.setBiometric(true)
        g.deviceAuth.result = false
        runBlocking { g.repository.putSetting(SettingKeys.PROFILE_NAME, "Carla") }
        setContent { LomaApp(g.graph, darkTheme = false, showIntro = false) }
        waitUntil(timeoutMillis = 5_000) { runCatching { onNodeWithTag("lock_title").assertTextContains("Carla", substring = true) }.isSuccess }
        onNodeWithTag("pin_key_1").performClick()
        onNodeWithTag("pin_key_2").performClick()
        waitForIdle()
        onRoot().captureRoboImage("screenshots/bloqueio.png")
    }

    @Test
    fun seguranca() = runDesktopComposeUiTest(width = 390, height = 844) {
        val g = TestGraph(pin = "1234")
        setContent { TestHost(g.graph) { SecurityScreen() } }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("security_change_pin")).fetchSemanticsNodes().isNotEmpty() }
        onRoot().captureRoboImage("screenshots/seguranca.png")
    }
}
