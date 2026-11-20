// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.nav_label
import app.loma.ui.generated.resources.tab_agenda
import app.loma.ui.generated.resources.tab_clientes
import app.loma.ui.generated.resources.tab_financeiro
import app.loma.ui.generated.resources.tab_inicio
import app.loma.ui.theme.LocalLomaColors
import app.loma.ui.theme.LocalLomaTypography
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal val LomaTab.label: StringResource
    get() = when (this) {
        LomaTab.Inicio -> Res.string.tab_inicio
        LomaTab.Agenda -> Res.string.tab_agenda
        LomaTab.Clientes -> Res.string.tab_clientes
        LomaTab.Financeiro -> Res.string.tab_financeiro
    }

private val LomaTab.icon: ImageVector
    get() = when (this) {
        LomaTab.Inicio -> LomaIcons.Casa
        LomaTab.Agenda -> LomaIcons.Calendario
        LomaTab.Clientes -> LomaIcons.Pessoas
        LomaTab.Financeiro -> LomaIcons.Carteira
    }

private val BarShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** Barra inferior da tela aprovada T-Inicio: 84 dp, cantos de 28 dp, indicador 56×32 dp. */
@Composable
fun LomaBottomBar(selected: LomaTab, onSelect: (LomaTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalLomaColors.current
    val navLabel = stringResource(Res.string.nav_label)
    Box(
        modifier
            .fillMaxWidth()
            .shadow(20.dp, BarShape, ambientColor = Color(0x1217165C), spotColor = Color(0x1217165C))
            .background(colors.surface, BarShape)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 16.dp)
                .semantics { contentDescription = navLabel }
                .selectableGroup(),
            verticalAlignment = Alignment.Top,
        ) {
            LomaTab.entries.forEach { tab ->
                BarItem(tab, tab == selected, onSelect, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BarItem(tab: LomaTab, isSelected: Boolean, onSelect: (LomaTab) -> Unit, modifier: Modifier) {
    val colors = LocalLomaColors.current
    val style = LocalLomaTypography.current.navLabel
    Column(
        modifier
            .testTag("tab_" + tab.name.lowercase())
            .heightIn(min = 48.dp)
            .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp)
                .background(if (isSelected) colors.navIndicator else Color.Transparent, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                tab.icon,
                contentDescription = null,
                tint = if (isSelected) colors.navIndicatorIcon else colors.textSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            stringResource(tab.label),
            style = style.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (isSelected) colors.textPrimary else colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
