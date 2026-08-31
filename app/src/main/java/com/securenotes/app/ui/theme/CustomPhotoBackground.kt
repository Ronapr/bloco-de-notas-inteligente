package com.securenotes.app.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CustomPhotoBackground(
    photoUri: String,
    modifier: Modifier = Modifier,
    overlayAlpha: Float = 0.42f
) {
    val context = LocalContext.current
    Box(modifier.fillMaxSize().background(Color(0xFF14101C))) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val gw = constraints.maxWidth.coerceAtMost(900)
            val gh = constraints.maxHeight.coerceAtMost(1600)
            var bmp by remember(photoUri, gw, gh) { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(photoUri, gw, gh) {
                if (photoUri.isBlank()) {
                    bmp = null
                    return@LaunchedEffect
                }
                bmp = withContext(Dispatchers.IO) {
                    try {
                        val uri = Uri.parse(photoUri)
                        fun openStream(u: Uri) = try {
                            if (u.scheme == "file") {
                                val p = u.path ?: u.toString().removePrefix("file://")
                                File(p).inputStream()
                            } else {
                                context.contentResolver.openInputStream(u)
                            }
                        } catch (_: Throwable) { null }
                        var sample = 1
                        try {
                            openStream(uri)?.use { input ->
                                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                BitmapFactory.decodeStream(input, null, opts)
                                val imgW = opts.outWidth
                                val imgH = opts.outHeight
                                if (imgW > 0 && imgH > 0) {
                                    val targetW = gw.coerceAtLeast(720)
                                    val targetH = gh.coerceAtLeast(1280)
                                    var s = 1
                                    while (imgW / s > targetW * 1.5 || imgH / s > targetH * 1.5) s *= 2
                                    sample = s
                                }
                            }
                        } catch (_: Throwable) {}
                        try {
                            openStream(uri)?.use { input2 ->
                                val opts2 = BitmapFactory.Options().apply { inSampleSize = sample }
                                BitmapFactory.decodeStream(input2, null, opts2)
                            }
                        } catch (_: Throwable) { null }
                    } catch (_: Throwable) {
                        null
                    }
                }
            }

            bmp?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // Camada escura translúcida para legibilidade
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha))
            )
        }
    }
}
