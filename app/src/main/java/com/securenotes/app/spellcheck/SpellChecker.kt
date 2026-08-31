package com.securenotes.app.spellcheck

interface SpellChecker {

    fun isReady(): Boolean

    suspend fun awaitReady(): Boolean = isReady()

    fun checkWord(word: String): List<String>?

    fun addToDictionary(word: String) {}

    fun scan(text: String): List<SpellingIssue> {
        val issues = ArrayList<SpellingIssue>()
        for (match in WORD_REGEX.findAll(text)) {
            val suggestions = checkWord(match.value)
            if (suggestions != null) {
                issues.add(
                    SpellingIssue(
                        offset = match.range.first,
                        length = match.value.length,
                        word = match.value,
                        suggestions = suggestions
                    )
                )
            }
        }
        return issues
    }

    companion object {
        val WORD_REGEX = Regex("[\\p{L}]+(?:-\\p{L}+)*")
    }
}

object NoOpSpellChecker : SpellChecker {
    override fun isReady(): Boolean = false

    override suspend fun awaitReady(): Boolean = false

    override fun checkWord(word: String): List<String>? = null
}