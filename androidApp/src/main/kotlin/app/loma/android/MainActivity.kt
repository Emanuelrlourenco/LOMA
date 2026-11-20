// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.android

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.loma.ui.shell.LomaApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as LomaApplication).graph
        // Ícones da barra de status seguem o tema do app (o botão sol/lua pode diferir do celular).
        setContent { LomaApp(graph, onDarkThemeChange = ::systemBars) }
    }

    private fun systemBars(dark: Boolean) = enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
        navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
    )

    private companion object {
        // Mesmos véus padrão do enableEdgeToEdge para a navegação de 3 botões.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xff, 0xff, 0xff)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
