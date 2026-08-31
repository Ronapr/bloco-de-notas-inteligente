package com.securenotes.app.spellcheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSpellCheckEngineTest {

    private val engine = OfflineSpellCheckEngine.fromWords(
        listOf(
            "casa", "casas", "computador", "computadores", "também", "país", "órgão",
            "coração", "corações", "escrever", "escrevi", "escreveu", "ação", "ações",
            "rapidamente", "trabalhávamos", "nível", "área", "capacidade", "feito",
            "disse", "tive", "pôr", "protegi", "protege", "conheço", "extra",
            "extra-abdominal", "a", "o", "é", "não", "mais", "de", "que"
        )
    )

    @Test
    fun `known words are not flagged`() {
        assertNull(engine.checkWord("casa"))
        assertNull(engine.checkWord("casa".uppercase().lowercase()))
        assertNull(engine.checkWord("computadores"))
        assertNull(engine.checkWord("trabalhávamos"))
        assertNull(engine.checkWord("extra-abdominal"))
        assertNull(engine.checkWord("Casa"))
        assertNull(engine.checkWord("TAMBÉM"))
    }

    @Test
    fun `unknown word is flagged with substitution suggestion`() {
        val suggestions = engine.checkWord("cazas")
        assertNotNull(suggestions)
        assertTrue(suggestions!!.contains("casas"))
    }

    @Test
    fun `unknown word is flagged with deletion suggestion`() {
        val suggestions = engine.checkWord("computadr")
        assertNotNull(suggestions)
        assertTrue(suggestions!!.contains("computador"))
    }

    @Test
    fun `unknown word is flagged with transposition suggestion`() {
        val suggestions = engine.checkWord("compuatdor")
        assertNotNull(suggestions)
        assertTrue(suggestions!!.contains("computador"))
    }

    @Test
    fun `de-accented word receives accented suggestion`() {
        val suggestions = engine.checkWord("tambem")
        assertNotNull(suggestions)
        assertTrue(suggestions!!.contains("também"))

        val pais = engine.checkWord("pais")
        assertNotNull(pais)
        assertTrue(pais!!.contains("país"))

        val orgao = engine.checkWord("orgao")
        assertNotNull(orgao)
        assertTrue(orgao!!.contains("órgão"))
    }

    @Test
    fun `distant word is flagged without suggestions`() {
        val suggestions = engine.checkWord("zxqwpl")
        assertNotNull(suggestions)
        assertTrue(suggestions!!.isEmpty())
    }

    @Test
    fun `word added to dictionary is no longer flagged`() {
        assertNotNull(engine.checkWord("jornada"))
        engine.addToDictionary("jornada")
        assertNull(engine.checkWord("jornada"))
        assertNull(engine.checkWord("Jornada"))
    }

    @Test
    fun `punctuation and numbers are skipped`() {
        assertNull(engine.checkWord(""))
        assertNull(engine.checkWord("123"))
        assertNull(engine.checkWord("casa!"))
    }

    @Test
    fun `scan returns issues with correct offsets`() {
        val issues = engine.scan("casa cassas computador compuatdor")
        assertEquals(2, issues.size)
        assertEquals("cassas", issues[0].word)
        assertEquals(5, issues[0].offset)
        assertEquals(6, issues[0].length)
        assertTrue(issues[0].suggestions.contains("casas"))
        assertEquals("compuatdor", issues[1].word)
        assertEquals(23, issues[1].offset)
        assertEquals(10, issues[1].length)
        assertTrue(issues[1].suggestions.contains("computador"))
    }

    @Test
    fun `scan ignores valid words`() {
        val issues = engine.scan("a casa é não mais")
        assertTrue(issues.isEmpty())
    }
}