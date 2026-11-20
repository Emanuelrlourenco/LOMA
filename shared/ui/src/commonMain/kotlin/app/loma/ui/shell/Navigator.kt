// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf

/** Pilha de telas por cima das abas; vazia = abas. */
class Navigator(initial: List<Route> = emptyList()) {
    private val entries = mutableStateListOf<Route>().apply { addAll(initial) }

    val stack: List<Route> get() = entries

    fun push(r: Route) {
        entries.add(r)
    }

    fun pop(): Boolean {
        if (entries.isEmpty()) return false
        entries.removeAt(entries.lastIndex)
        return true
    }

    fun replaceTop(r: Route) {
        if (entries.isNotEmpty()) entries.removeAt(entries.lastIndex)
        entries.add(r)
    }

    companion object {
        val Saver = listSaver<Navigator, String>(
            save = { nav -> nav.stack.map { it.encode() } },
            restore = { saved -> Navigator(saved.map { Route.decode(it) }) },
        )
    }
}

@Composable
fun rememberNavigator(): Navigator = rememberSaveable(saver = Navigator.Saver) { Navigator() }

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Navigator não fornecido") }
