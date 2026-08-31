package com.securenotes.app.ui

import androidx.compose.ui.graphics.Color
import com.securenotes.app.R

data class NoteColorOption(
    val key: String,
    val color: Color?,
    val labelRes: Int
)

object NoteColors {

    const val SEM_COR = ""
    const val AZUL = "azul"
    const val VERDE = "verde"
    const val AMARELO = "amarelo"
    const val LARANJA = "laranja"
    const val VERMELHO = "vermelho"
    const val ROXO = "roxo"

    val options: List<NoteColorOption> = listOf(
        NoteColorOption(SEM_COR, null, R.string.sem_cor),
        NoteColorOption(AZUL, Color(0xFF2196F3), R.string.cor_azul),
        NoteColorOption(VERDE, Color(0xFF43A047), R.string.cor_verde),
        NoteColorOption(AMARELO, Color(0xFFFDD835), R.string.cor_amarelo),
        NoteColorOption(LARANJA, Color(0xFFFB8C00), R.string.cor_laranja),
        NoteColorOption(VERMELHO, Color(0xFFE53935), R.string.cor_vermelho),
        NoteColorOption(ROXO, Color(0xFF8E24AA), R.string.cor_roxo)
    )

    fun colorOf(key: String): Color? =
        options.firstOrNull { it.key == key }?.color
}
