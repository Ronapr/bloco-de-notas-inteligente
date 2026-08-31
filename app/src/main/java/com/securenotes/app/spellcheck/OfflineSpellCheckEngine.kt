package com.securenotes.app.spellcheck

import android.content.res.AssetManager
import java.util.concurrent.ConcurrentHashMap

class OfflineSpellCheckEngine private constructor(
    private val bytes: ByteArray,
    private val starts: IntArray
) : SpellChecker {

    private val userWords: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val lineCount = starts.size

    companion object {
        private const val MAX_SUGGESTIONS = 6

        private val ACCENTS: Map<Char, List<Char>> = mapOf(
            'a' to listOf('á', 'à', 'â', 'ã'),
            'e' to listOf('é', 'ê'),
            'i' to listOf('í'),
            'o' to listOf('ó', 'ô', 'õ'),
            'u' to listOf('ú'),
            'c' to listOf('ç')
        )

        private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz"

        fun fromAsset(assets: AssetManager, assetName: String): OfflineSpellCheckEngine {
            val bytes = assets.open(assetName).use { it.readBytes() }
            return fromUtf8(bytes)
        }

        fun fromUtf8(bytes: ByteArray): OfflineSpellCheckEngine = fromSorted(bytes)

        fun fromWords(words: Iterable<String>): OfflineSpellCheckEngine {
            val clean = words
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
                .toSortedSet()
            val builder = StringBuilder()
            clean.forEach { builder.append(it).append('\n') }
            return fromSorted(builder.toString().toByteArray(Charsets.UTF_8))
        }

        private fun fromSorted(bytes: ByteArray): OfflineSpellCheckEngine {
            val starts = ArrayList<Int>()
            var lineStart = 0
            var i = 0
            val n = bytes.size
            while (i < n) {
                if (bytes[i] == '\n'.code.toByte()) {
                    if (i > lineStart) starts.add(lineStart)
                    lineStart = i + 1
                }
                i++
            }
            if (lineStart < n) starts.add(lineStart)
            return OfflineSpellCheckEngine(bytes, starts.toIntArray())
        }
    }

    init {
        check(lineCount > 0) { "dictionary is empty" }
    }

    override fun isReady(): Boolean = true

    override suspend fun awaitReady(): Boolean = true

    override fun checkWord(word: String): List<String>? {
        if (word.isEmpty()) return null
        if (word.any { !it.isLetter() && it != '-' }) return null
        val lower = word.lowercase()
        if (userWords.contains(lower)) return null
        if (contains(lower)) return null
        val suggestions = LinkedHashSet<String>()
        for (variant in accentVariants(lower)) suggestions.add(variant)
        for (candidate in editCandidates(lower)) {
            if (candidate != lower && contains(candidate) && suggestions.size < MAX_SUGGESTIONS) {
                suggestions.add(candidate)
            }
        }
        return suggestions.take(MAX_SUGGESTIONS)
    }

    override fun addToDictionary(word: String) {
        val w = word.trim().lowercase()
        if (w.isNotEmpty() && w.none { !it.isLetter() && it != '-' }) {
            userWords.add(w)
        }
    }

    private fun contains(word: String): Boolean {
        val needle = word.toByteArray(Charsets.UTF_8)
        var lo = 0
        var hi = lineCount - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val cmp = compareLine(starts[mid], needle)
            when {
                cmp < 0 -> lo = mid + 1
                cmp > 0 -> hi = mid - 1
                else -> return true
            }
        }
        return false
    }

    private fun compareLine(offset: Int, needle: ByteArray): Int {
        var i = offset
        var j = 0
        val n = needle.size
        while (j < n && i < bytes.size && bytes[i] != '\n'.code.toByte() && bytes[i] != '\r'.code.toByte()) {
            val a = bytes[i].toInt() and 0xFF
            val b = needle[j].toInt() and 0xFF
            if (a != b) return a - b
            i++
            j++
        }
        val lineEnded = i >= bytes.size || bytes[i] == '\n'.code.toByte() || bytes[i] == '\r'.code.toByte()
        if (j == n && lineEnded) return 0
        return if (j == n) 1 else -1
    }

    private fun accentVariants(word: String): List<String> {
        val positions = word.mapIndexedNotNull { i, c -> if (ACCENTS.containsKey(c)) i to c else null }
        if (positions.isEmpty()) return emptyList()
        val limited = if (positions.size > 8) positions.subList(0, 8) else positions
        val result = ArrayList<String>()
        var branches = 0

        fun dfs(index: Int, current: CharArray, changed: Int) {
            if (result.size >= 20 || branches >= 2000) return
            if (index == limited.size) {
                if (changed > 0) {
                    branches++
                    val candidate = String(current)
                    if (contains(candidate)) result.add(candidate)
                }
                return
            }
            val (position, original) = limited[index]
            branches++
            current[position] = original
            dfs(index + 1, current, changed)
            for (replacement in ACCENTS.getValue(original)) {
                branches++
                current[position] = replacement
                dfs(index + 1, current, changed + 1)
            }
        }

        dfs(0, word.toCharArray(), 0)
        return result
    }

    private fun editCandidates(word: String): List<String> {
        val out = ArrayList<String>()
        val chars = word.toCharArray()
        for (i in chars.indices) {
            if (chars.size > 1) {
                out.add(String(chars, 0, i) + String(chars, i + 1, chars.size - i - 1))
            }
        }
        for (i in 0 until chars.size - 1) {
            val first = chars[i]
            chars[i] = chars[i + 1]
            chars[i + 1] = first
            out.add(String(chars))
            val second = chars[i]
            chars[i] = chars[i + 1]
            chars[i + 1] = second
        }
        for (i in chars.indices) {
            val original = chars[i]
            for (c in ALPHABET) {
                if (c != original) {
                    chars[i] = c
                    out.add(String(chars))
                }
            }
            chars[i] = original
        }
        for (i in 0..chars.size) {
            val prefix = String(chars, 0, i)
            val suffix = String(chars, i, chars.size - i)
            for (c in ALPHABET) {
                out.add(prefix + c + suffix)
            }
        }
        return out
    }
}