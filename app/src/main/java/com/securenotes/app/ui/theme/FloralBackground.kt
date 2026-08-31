package com.securenotes.app.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.securenotes.app.R

/**
 * Floral Rosa — usa textura de alta resolução com rosas reais.
 * Imagem em res/drawable/floral_bg.png (1080x1920).
 * Painéis da interface são translúcidos para flores aparecerem atrás.
 */
@Composable
fun FloralBackground(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color(0xFFFFE8F0))) {
        Image(
            painter = painterResource(R.drawable.floral_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
