// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.shell

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Ícones da barra, traçados a partir dos SVG da tela aprovada T-Inicio (24×24, só contorno). */
internal object LomaIcons {
    val Casa = icon("casa", 2f, StrokeCap.Butt, StrokeJoin.Round,
        "M4 11l8-7 8 7v8a1 1 0 0 1-1 1h-4v-6H9v6H5a1 1 0 0 1-1-1z")
    val Calendario = icon("calendario", 1.9f, StrokeCap.Round, StrokeJoin.Miter,
        "M7 5h10a3 3 0 0 1 3 3v9a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z",
        "M4 10h16M9 3v4M15 3v4")
    val Pessoas = icon("pessoas", 1.9f, StrokeCap.Round, StrokeJoin.Miter,
        "M5.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0z",
        "M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6",
        "M16 4.5a3.5 3.5 0 0 1 0 7M21 20c0-2.6-1.6-4.8-4-5.6")
    val Carteira = icon("carteira", 1.9f, StrokeCap.Round, StrokeJoin.Miter,
        "M6 6h12a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V9a3 3 0 0 1 3-3z",
        "M3 10h18",
        "M15.3 15a1.2 1.2 0 1 0 2.4 0a1.2 1.2 0 1 0-2.4 0z")

    val Voltar = icon("voltar", 2.2f, StrokeCap.Round, StrokeJoin.Round, "M15 5l-7 7 7 7")
    val Subir = icon("subir", 2.2f, StrokeCap.Round, StrokeJoin.Round, "M6 15l6-6 6 6")
    val Descer = icon("descer", 2.2f, StrokeCap.Round, StrokeJoin.Round, "M6 9l6 6 6-6")
    val Mais = icon("mais", 2.4f, StrokeCap.Round, StrokeJoin.Round, "M12 5v14M5 12h14")
    val Sino = icon("sino", 1.9f, StrokeCap.Round, StrokeJoin.Round, "M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20.5a2 2 0 0 0 4 0")
    val Seta = icon("seta", 2f, StrokeCap.Round, StrokeJoin.Round, "M9 6l6 6-6 6")

    private fun icon(name: String, width: Float, cap: StrokeCap, join: StrokeJoin, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach {
            builder.addPath(
                pathData = addPathNodes(it),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = cap,
                strokeLineJoin = join,
            )
        }
        return builder.build()
    }
}
