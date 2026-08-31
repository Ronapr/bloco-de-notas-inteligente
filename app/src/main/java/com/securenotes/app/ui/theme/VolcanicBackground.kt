package com.securenotes.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

// ============================================================
// LAVA / OBSIDIANA — 2.5D rocha com profundidade real
// NÃO desenha linhas. Usa height map + ridge fissures +
// emissive + AO + iluminação — cavidades profundas com
// magma abaixo da crosta.
// ============================================================

private fun hash2(x: Int, y: Int): Float {
    var h = x * 374761393 + y * 668265263
    h = (h xor (h shr 13)) * 1274126177
    return ((h xor (h shr 16)) and 0x7fffffff) / 1073741824f - 1f
}

private fun valueNoise(x: Float, y: Float): Float {
    val xi = kotlin.math.floor(x).toInt()
    val yi = kotlin.math.floor(y).toInt()
    val xf = x - xi
    val yf = y - yi
    val u = xf * xf * (3f - 2f * xf)
    val v = yf * yf * (3f - 2f * yf)
    val n00 = hash2(xi, yi)
    val n10 = hash2(xi + 1, yi)
    val n01 = hash2(xi, yi + 1)
    val n11 = hash2(xi + 1, yi + 1)
    return (n00 * (1f - u) + n10 * u) * (1f - v) + (n01 * (1f - u) + n11 * u) * v
}

private fun fbm(x: Float, y: Float, octaves: Int = 4): Float {
    var v = 0f
    var amp = 0.5f
    var freq = 1f
    var maxAmp = 0f
    repeat(octaves) {
        v += valueNoise(x * freq, y * freq) * amp
        maxAmp += amp
        amp *= 0.5f
        freq *= 2f
    }
    return v / maxAmp
}

private fun ridgeNoise(x: Float, y: Float): Float {
    val n = fbm(x, y, 4)
    return 1f - abs(n) // 0..1, fissura onde ~1
}

private fun smoothstep(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun lerpColor(c1: Int, c2: Int, t: Float): Int {
    val r = lerp((c1 shr 16 and 0xFF).toFloat(), (c2 shr 16 and 0xFF).toFloat(), t).toInt()
    val g = lerp((c1 shr 8 and 0xFF).toFloat(), (c2 shr 8 and 0xFF).toFloat(), t).toInt()
    val b = lerp((c1 and 0xFF).toFloat(), (c2 and 0xFF).toFloat(), t).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

private fun generateLavaBitmap(w: Int, h: Int): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(w * h)

    // Paleta explícita
    val rockBlack = 0xFF050505.toInt()
    val rockGray = 0xFF171717.toInt()
    // Lava degradê: 7A0800 → FF1600 → FF5A00 → FF9D00 → FFD000
    val lavaDeep = 0xFF7A0800.toInt()
    val lavaRed = 0xFFFF1600.toInt()
    val lavaOrange = 0xFFFF5A00.toInt()
    val lavaHot = 0xFFFF9D00.toInt()
    val lavaYellow = 0xFFFFD000.toInt()

    for (y in 0 until h) {
        for (x in 0 until w) {
            val xf = x.toFloat()
            val yf = y.toFloat()

            // Domain warping — distorce fissuras para orgânico (sem repetir)
            val warpX = fbm(xf * 0.006f, yf * 0.006f, 3) * 14f
            val warpY = fbm(xf * 0.006f + 100f, yf * 0.006f + 100f, 3) * 14f
            val wx = xf + warpX
            val wy = yf + warpY

            // Height map da rocha (relevo)
            val h0 = fbm(wx * 0.010f, wy * 0.010f, 4) // grande escala — ondulações
            val h1 = fbm(wx * 0.045f, wy * 0.045f, 3) * 0.35f // detalhe médio
            val h2 = hash2(x, y) * 0.08f // granulação fina
            val height = (h0 + h1 + h2).coerceIn(-1f, 1f)

            // Normal aproximado para iluminação (diferença finita de height)
            val hx1 = fbm((wx + 1f) * 0.010f, wy * 0.010f, 4)
            val hx2 = fbm((wx - 1f) * 0.010f, wy * 0.010f, 4)
            val hy1 = fbm(wx * 0.010f, (wy + 1f) * 0.010f, 4)
            val hy2 = fbm(wx * 0.010f, (wy - 1f) * 0.010f, 4)
            val nx = (hx1 - hx2) * 8f
            val ny = (hy1 - hy2) * 8f
            val nz = 1f
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            // Luz direcional suave de cima-esquerda
            val lx = -0.35f; val ly = -0.55f; val lz = 0.76f
            val diff = ((nx * lx + ny * ly + nz * lz) / len).coerceIn(-0.4f, 1f) * 0.5f + 0.5f
            val diffuse = 0.62f + diff * 0.38f

            // Ridge fissures — cavidades finas naturais
            val ridge = ridgeNoise(wx * 0.018f, wy * 0.018f)
            // Densidade variável (algumas áreas sem fissura) — grande escala
            val density = fbm(wx * 0.004f, wy * 0.004f, 3) * 0.08f // -0.08 .. 0.08
            val ridgeBiased = (ridge + density).coerceIn(0f, 1f)

            // Thresholds: fissura fina 0.93-1.0 (5-8% área), glow 0.86-0.93, outer glow 0.81-0.86
            val crackT = 0.935f
            val glowT1 = 0.885f
            val glowT0 = 0.815f

            val vignette = (1f - run {
                val u = (xf / w - 0.5f) * 2f
                val v = (yf / h - 0.5f) * 2f
                (u * u + v * v) * 0.18f
            }).coerceIn(0.72f, 1f)

            val col: Int = when {
                ridgeBiased >= crackT -> {
                    // ── DENTRO DA CAVIDADE — lava emissiva no fundo ──
                    val t = ((ridgeBiased - crackT) / (1f - crackT)).coerceIn(0f, 1f)
                    // Profundidade: borda da cavidade mais escura (paredes), fundo incandescente
                    // Parede: 0-0.25 ainda sombra, 0.25+ lava aparece
                    if (t < 0.22f) {
                        // Parede da cavidade — escuro com leve vermelho
                        val wallT = t / 0.22f
                        lerpColor(0xFF1A0500.toInt(), lavaDeep, wallT)
                    } else {
                        val lt = ((t - 0.22f) / 0.78f).coerceIn(0f, 1f)
                        when {
                            lt < 0.30f -> lerpColor(lavaDeep, lavaRed, lt / 0.30f)
                            lt < 0.55f -> lerpColor(lavaRed, lavaOrange, (lt - 0.30f) / 0.25f)
                            lt < 0.78f -> lerpColor(lavaOrange, lavaHot, (lt - 0.55f) / 0.23f)
                            else -> {
                                val tt = (lt - 0.78f) / 0.22f
                                val c = lerpColor(lavaHot, lavaYellow, tt)
                                // Núcleo quase branco no mais quente (pequeno)
                                if (tt > 0.75f) lerpColor(c, 0xFFFFF6C0.toInt(), (tt - 0.75f) / 0.25f) else c
                            }
                        }
                    }
                }
                ridgeBiased >= glowT1 -> {
                    // ── BORDA ILUMINADA — luz da lava refletindo na rocha (emissive + AO) ──
                    val t = (ridgeBiased - glowT1) / (crackT - glowT1) // 0..1
                    // Rock base com altura
                    val rockLum = lerp(5f, 23f, (height + 1f) * 0.5f)
                    val baseR = (rockLum * diffuse).toInt().coerceIn(5, 40)
                    val baseG = (rockLum * 0.88f * diffuse).toInt().coerceIn(5, 36)
                    val baseB = (rockLum * 0.82f * diffuse).toInt().coerceIn(5, 34)
                    // Glow quente sobre a rocha — intenso perto da fissura, cai rápido
                    val glowR = (lerp(90f, 10f, 1f - t) * t).toInt()
                    val glowG = (lerp(35f, 2f, 1f - t) * t * 0.6f).toInt()
                    val r = (baseR + glowR).coerceAtMost(180)
                    val g = (baseG + glowG).coerceAtMost(100)
                    val b = baseB
                    // Bloom sutil — clareia geral perto da lava
                    val bloom = (t * 18f).toInt()
                    (0xFF shl 24) or (((r + bloom).coerceAtMost(255) * vignette).toInt() shl 16) or (((g + bloom * 0.4f).toInt().coerceAtMost(255) * vignette).toInt() shl 8) or ((b * vignette).toInt())
                }
                ridgeBiased >= glowT0 -> {
                    // ── HALO EXTERNO — luz difusa distante ──
                    val t = (ridgeBiased - glowT0) / (glowT1 - glowT0)
                    val ao = 1f - smoothstep(glowT0, glowT1, ridgeBiased) * 0.35f // AO escurece cavidade
                    val rockLum = lerp(5f, 23f, (height + 1f) * 0.5f) * ao
                    val baseR = (rockLum * diffuse).toInt().coerceIn(5, 36)
                    val baseG = (rockLum * 0.88f * diffuse).toInt().coerceIn(5, 34)
                    val baseB = (rockLum * 0.82f * diffuse).toInt().coerceIn(5, 32)
                    val glowR = (18f * t * t).toInt()
                    (0xFF shl 24) or (((baseR + glowR) * vignette).toInt() shl 16) or ((baseG * vignette).toInt() shl 8) or ((baseB * vignette).toInt())
                }
                else -> {
                    // ── ROCHA PURA — áspera com relevo ──
                    val ao = 1f - smoothstep(glowT0 - 0.04f, glowT0, ridgeBiased) * 0.25f
                    val rockLum = lerp(5f, 23f, (height + 1f) * 0.5f) * ao
                    // Variação fina fragmentada
                    val frag = (hash2(x * 2, y * 2) * 2f).coerceIn(-2f, 2f)
                    val r = (rockLum * diffuse + frag).toInt().coerceIn(5, 38)
                    val g = (rockLum * 0.88f * diffuse + frag * 0.7f).toInt().coerceIn(5, 34)
                    val b = (rockLum * 0.80f * diffuse + frag * 0.4f).toInt().coerceIn(5, 32)
                    (0xFF shl 24) or ((r * vignette).toInt() shl 16) or ((g * vignette).toInt() shl 8) or ((b * vignette).toInt())
                }
            }
            pixels[y * w + x] = col
        }
    }
    bmp.setPixels(pixels, 0, w, 0, 0, w, h)
    return bmp
}

@Composable
fun VolcanicBackground(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color(0xFF050505))) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val gw = constraints.maxWidth.coerceAtMost(480)
            val gh = constraints.maxHeight.coerceAtMost(900)
            var bmp by remember(gw, gh) { mutableStateOf<Bitmap?>(null) }
            LaunchedEffect(gw, gh) {
                bmp = withContext(Dispatchers.Default) { generateLavaBitmap(gw, gh) }
            }
            bmp?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
