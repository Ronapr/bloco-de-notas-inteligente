package com.securenotes.app.spellcheck

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Teste do dicionário real (asset) carregado em ambiente JVM.
 */
class DebugAssetTest {

    @Test
    fun scanRealAssetFlagsOnlyMisspelledWords() {
        val bytes = File("C:/projeto/BlocoDeNotas/app/src/main/assets/pt_br_dict.txt").readBytes()
        val engine = OfflineSpellCheckEngine.fromUtf8(bytes)

        val issues = engine.scan("Comprar insumos da cozinha  cassas computadorr tambem")

        assertEquals(2, issues.size)
        assertEquals("computadorr", issues[0].word)
        assertEquals(35, issues[0].offset)
        assertEquals("tambem", issues[1].word)
        assertEquals(47, issues[1].offset)
        assertTrue(issues[0].suggestions.contains("computador"))
        assertTrue(issues[1].suggestions.contains("também"))
    }

    @Test
    fun scanRealAssetAcceptsKnownWords() {
        val bytes = File("C:/projeto/BlocoDeNotas/app/src/main/assets/pt_br_dict.txt").readBytes()
        val engine = OfflineSpellCheckEngine.fromUtf8(bytes)

        val issues = engine.scan("Comprar insumos da cozinha computador também")

        assertTrue(issues.isEmpty())
    }
}