// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.clients

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.loma.ui.LocalLomaGraph
import app.loma.ui.theme.LomaPalette
import app.loma.ui.theme.LocalLomaTypography
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Iniciais: primeira letra do primeiro e do último nome ("MS"). */
fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/** Foto ou iniciais em fundo lilás (68 dp, cantos 24 na ficha). */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun ClientAvatar(name: String, photoFile: String?, size: Int = 68, modifier: Modifier = Modifier) {
    val photos = LocalLomaGraph.current.photos
    val bitmap by produceState<ImageBitmap?>(null, photoFile) {
        value = photoFile?.let { photos.read(it) }?.takeIf { it.isNotEmpty() }?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
    }
    val shape = RoundedCornerShape((size * 24 / 68).dp)
    Box(modifier.size(size.dp).clip(shape).background(LomaPalette.Lilac, shape), contentAlignment = Alignment.Center) {
        val b = bitmap
        if (b != null) {
            Image(b, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size.dp).testTag("avatar_photo"))
        } else {
            Text(
                initials(name),
                style = LocalLomaTypography.current.body.copy(fontWeight = FontWeight.ExtraBold, fontSize = (size * 24 / 68).sp),
                color = LomaPalette.Navy,
            )
        }
    }
}
