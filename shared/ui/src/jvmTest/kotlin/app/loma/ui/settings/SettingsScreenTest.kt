// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import app.loma.data.SettingKeys
import app.loma.ui.TestGraph
import app.loma.ui.TestHost
import app.loma.ui.testPng
import app.loma.ui.waitForData
import java.util.Locale
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    @BeforeTest
    fun idioma() = Locale.setDefault(Locale.forLanguageTag("pt-BR"))

    @Test
    fun removerFotoDoPerfil() = runComposeUiTest {
        val g = TestGraph()
        g.photos.files["perfil.jpg"] = testPng()
        runBlocking { g.repository.putSetting(SettingKeys.PROFILE_PHOTO, "perfil.jpg") }
        setContent { TestHost(g.graph) { SettingsScreen() } }
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("avatar_photo")).fetchSemanticsNodes().isNotEmpty() }

        onNodeWithTag("profile_photo_remove").performScrollTo().performClick()

        waitForData { g.repository.setting(SettingKeys.PROFILE_PHOTO).first().isNullOrEmpty() }
        // O arquivo é apagado logo depois de gravar o ajuste, na mesma corrotina.
        waitUntil(timeoutMillis = 5_000) { g.photos.deleted.isNotEmpty() }
        assertEquals(listOf("perfil.jpg"), g.photos.deleted)
        waitUntil(timeoutMillis = 5_000) { onAllNodes(hasTestTag("avatar_photo")).fetchSemanticsNodes().isEmpty() }
    }
}
