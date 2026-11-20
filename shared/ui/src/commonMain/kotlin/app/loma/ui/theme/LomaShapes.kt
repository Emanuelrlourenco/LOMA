// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Raios das telas aprovadas: itens 16, cartões 26, folhas 32, botões em pílula. */
object LomaShapes {
    val Item = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(26.dp)
    val Sheet = RoundedCornerShape(32.dp)
    val Pill = CircleShape

    val material = Shapes(small = Item, medium = Card, large = Sheet)
}
