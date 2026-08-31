package com.securenotes.app.spellcheck

data class SpellingIssue(
    val offset: Int,
    val length: Int,
    val word: String,
    val suggestions: List<String>
) {
    fun contains(offsetInText: Int): Boolean = offsetInText in offset..(offset + length)
}