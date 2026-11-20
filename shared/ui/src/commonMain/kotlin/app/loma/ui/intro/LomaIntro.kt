// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 The LOMA Authors

package app.loma.ui.intro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.loma.ui.generated.resources.Res
import app.loma.ui.generated.resources.intro_desc
import app.loma.ui.generated.resources.intro_skip
import app.loma.ui.theme.LomaPalette
import app.loma.ui.theme.outfitFamily
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import kotlin.math.tan

/** Faísca do protótipo: deslocamento a partir do centro do brilho (dp), tamanho, cor, início e duração. */
private class Spark(val dx: Float, val dy: Float, val size: Float, val color: Color, val delay: Int, val duration: Int)

private val Sparks = listOf(
    Spark(-42.5f, 72.5f, 5f, LomaPalette.Mint, 200, 1700),
    Spark(45f, 52f, 4f, LomaPalette.Lilac, 500, 1700),
    Spark(-15.5f, 91.5f, 3f, Color.White, 800, 1600),
    Spark(21.5f, 82.5f, 5f, LomaPalette.Mint, 1050, 1500),
    Spark(-61f, 57f, 4f, LomaPalette.Lilac, 1300, 1400),
)

/** Letras do nome e o atraso de cada uma (o "o" menta entra com escala). */
private val Letters = listOf('l' to 2350, 'o' to 2520, 'm' to 2650, 'a' to 2800)

/** Centro do brilho em altura relativa da tela (350 de 844 no protótipo). */
private const val ANCHOR_Y = 350f / 844f

/**
 * Abertura animada do LOMA (design §7.12b): brilho menta, faíscas, o ponto que vira o "o" menta,
 * as letras l·m·a subindo, um reflexo e a saída. Um toque em qualquer ponto pula na hora.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun LomaIntro(soundEnabled: Boolean, reducedMotion: Boolean, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val sound = rememberIntroSound()
    val latestOnFinished by rememberUpdatedState(onFinished)
    val done = remember { mutableStateOf(false) }
    val elapsed = remember { mutableLongStateOf(if (reducedMotion) 3_400L else 0L) }
    val total = if (reducedMotion) IntroTimeline.REDUCED_MOTION_MS else IntroTimeline.TOTAL_MS

    fun finish() {
        if (done.value) return
        done.value = true
        sound.stop()
        latestOnFinished()
    }

    LaunchedEffect(Unit) {
        if (soundEnabled && !reducedMotion) {
            launch {
                val bytes = runCatching { Res.readBytes(introSoundFile) }.getOrNull()
                if (bytes != null && !done.value) sound.play(bytes)
            }
        }
        val start = withFrameMillis { it }
        while (!done.value) {
            val now = withFrameMillis { it }
            val passed = now - start
            // Sem animações, o nome fica parado (quadro final do nome) até o fim.
            if (!reducedMotion) elapsed.longValue = passed
            if (passed >= total) finish()
        }
    }
    DisposableEffect(Unit) { onDispose { sound.stop() } }

    val description = stringResource(Res.string.intro_desc)
    val skip = stringResource(Res.string.intro_skip)
    Box(
        modifier
            .fillMaxSize()
            .testTag("loma_intro")
            .semantics { contentDescription = description }
            .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = skip) { finish() },
    ) {
        IntroStage(elapsed)
    }
}

@Composable
private fun IntroStage(elapsed: MutableLongState) {
    val t = { elapsed.longValue }
    // Fundo marinho que se dissolve no fim (4100 ms, 450 ms).
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = keyframes(t(), 4100, 450, EaseIn, 0f to 1f, 1f to 0f) }
            .background(LomaPalette.Navy),
    )
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = keyframes(t(), IntroTimeline.EXIT_MS, 450, EaseIn, 0f to 1f, 1f to 0f)
                val s = keyframes(t(), IntroTimeline.EXIT_MS, 450, EaseIn, 0f to 1f, 1f to 0.92f)
                scaleX = s
                scaleY = s
            },
    ) {
        val anchorY = maxHeight * ANCHOR_Y
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * ANCHOR_Y)
            val now = t()
            // Brilho menta (160 dp com desfoque de 44 px no protótipo).
            val glowAlpha = keyframes(now, IntroTimeline.GLOW_MS, 3000, EaseIn, 0f to 0f, 0.7f to 0.55f, 0.8f to 0.75f, 1f to 0.25f)
            val glowScale = keyframes(now, IntroTimeline.GLOW_MS, 3000, EaseIn, 0f to 0.3f, 0.7f to 1f, 0.8f to 1.15f, 1f to 1.05f)
            val glowRadius = 140.dp.toPx() * glowScale
            if (glowAlpha > 0f && glowRadius > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0f to LomaPalette.Mint,
                        0.4f to LomaPalette.Mint.copy(alpha = 0.8f),
                        0.7f to LomaPalette.Mint.copy(alpha = 0.3f),
                        1f to Color.Transparent,
                        center = center,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = center,
                    alpha = glowAlpha,
                )
            }
            // Faíscas que sobem.
            Sparks.forEach { spark ->
                val a = keyframes(now, spark.delay, spark.duration, EaseOut, 0f to 0f, 0.5f to 0.85f, 1f to 0f)
                val dy = keyframes(now, spark.delay, spark.duration, EaseOut, 0f to 20f, 1f to -44f)
                if (a > 0f) {
                    drawCircle(
                        color = spark.color,
                        radius = spark.size.dp.toPx() / 2f,
                        center = center + Offset(spark.dx.dp.toPx(), (spark.dy + dy).dp.toPx()),
                        alpha = a,
                    )
                }
            }
            // Anel de luz no toque principal do som.
            val ringScale = keyframes(now, IntroTimeline.HIT_MS, 900, EaseOut, 0f to 0.3f, 1f to 4f)
            val ringAlpha = keyframes(now, IntroTimeline.HIT_MS, 900, EaseOut, 0f to 0.9f, 1f to 0f)
            if (ringAlpha > 0f) {
                val stroke = 3.dp.toPx() * ringScale
                drawCircle(
                    color = LomaPalette.Mint,
                    radius = (30.dp.toPx() * ringScale - stroke / 2f).coerceAtLeast(0f),
                    center = center,
                    alpha = ringAlpha,
                    style = Stroke(width = stroke),
                )
            }
        }
        IntroWord(t, Modifier.align(Alignment.TopCenter).offset(y = anchorY - 46.dp))
        // Ponto menta que "estoura" e vira o "o".
        Canvas(Modifier.fillMaxSize()) {
            val now = t()
            val inAlpha = keyframes(now, IntroTimeline.DOT_IN_MS, 950, DotEase, 0f to 0f, 0.64f to 1f, 1f to 1f)
            val inScale = keyframes(now, IntroTimeline.DOT_IN_MS, 950, DotEase, 0f to 0f, 0.64f to 0.6f, 0.79f to 1.35f, 1f to 1f)
            val outAlpha = keyframes(now, 2380, 420, EaseIn, 0f to 1f, 1f to 0f)
            val outScale = keyframes(now, 2380, 420, EaseIn, 0f to 1f, 1f to 1.6f)
            val dx = keyframes(now, 2380, 420, EaseIn, 0f to 0f, 1f to -36f)
            val alpha = (inAlpha * outAlpha).coerceIn(0f, 1f)
            if (alpha > 0f) {
                drawCircle(
                    color = LomaPalette.Mint,
                    radius = 11.dp.toPx() * inScale * outScale,
                    center = Offset(size.width / 2f + dx.dp.toPx(), size.height * ANCHOR_Y),
                    alpha = alpha,
                )
            }
        }
    }
}

/** O nome "loma" em Outfit ExtraBold 64, com o "o" menta — igual ao ícone 6B. */
@Composable
private fun IntroWord(t: () -> Long, modifier: Modifier) {
    val density = LocalDensity.current
    val style = TextStyle(
        fontFamily = outfitFamily(),
        fontWeight = FontWeight.ExtraBold,
        // Tamanho fixo em dp: o logo não cresce com a fonte do sistema.
        fontSize = with(density) { 64.dp.toSp() },
        letterSpacing = with(density) { (-1.6).dp.toSp() },
        lineHeight = 1.2.em,
    )
    Box(modifier.clipToBounds()) {
        Row(Modifier.padding(horizontal = 4.dp)) {
            Letters.forEach { (letter, delay) ->
                val isO = letter == 'o'
                Text(
                    letter.toString(),
                    style = style,
                    color = if (isO) LomaPalette.Mint else Color.White,
                    modifier = Modifier.graphicsLayer {
                        val now = t()
                        if (isO) {
                            alpha = keyframes(now, delay, 380, EaseOut, 0f to 0f, 1f to 1f)
                            val s = keyframes(now, delay, 380, EaseOut, 0f to 0.55f, 1f to 1f)
                            scaleX = s
                            scaleY = s
                        } else {
                            alpha = keyframes(now, delay, 420, EaseOut, 0f to 0f, 1f to 1f)
                            translationY = keyframes(now, delay, 420, EaseOut, 0f to 22f, 1f to 0f) * density.density
                        }
                    },
                )
            }
        }
        // Reflexo de luz (faixa de 40 dp inclinada 20°) que atravessa o nome.
        Canvas(Modifier.matchParentSize()) {
            val x = keyframes(t(), IntroTimeline.SHINE_MS, 600, EaseInOut, 0f to -160f, 1f to 260f).dp.toPx()
            val w = 40.dp.toPx()
            val skew = tan(20f * kotlin.math.PI.toFloat() / 180f) * size.height / 2f
            val band = Path().apply {
                moveTo(x + skew, 0f)
                lineTo(x + w + skew, 0f)
                lineTo(x + w - skew, size.height)
                lineTo(x - skew, size.height)
                close()
            }
            drawPath(band, Color.White.copy(alpha = 0.35f))
        }
    }
}
