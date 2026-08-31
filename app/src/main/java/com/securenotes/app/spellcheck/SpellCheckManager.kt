package com.securenotes.app.spellcheck

import android.content.Context
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpellCheckManager(context: Context) : SpellChecker {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val executor = Executors.newSingleThreadExecutor()

    private val engineFuture: Future<SpellChecker> = executor.submit(
        Callable {
            val offline = OfflineSpellCheckEngine.fromAsset(appContext.assets, DICT_ASSET)
            val saved = runCatching {
                prefs.getStringSet(USER_WORDS_KEY, emptySet())
            }.getOrNull() ?: emptySet()
            saved.forEach { offline.addToDictionary(it) }
            offline
        }
    )

    override fun isReady(): Boolean = engineFuture.isDone

    override suspend fun awaitReady(): Boolean = withContext(Dispatchers.IO) {
        try {
            engineFuture.get()
            true
        } catch (t: Throwable) {
            false
        }
    }

    override fun checkWord(word: String): List<String>? {
        if (!isReady()) return null
        return runCatching { engineFuture.get().checkWord(word) }.getOrNull()
    }

    override fun scan(text: String): List<SpellingIssue> {
        if (!isReady()) return emptyList()
        return runCatching { engineFuture.get().scan(text) }.getOrDefault(emptyList())
    }

    override fun addToDictionary(word: String) {
        if (!isReady()) return
        val w = word.trim().lowercase()
        if (w.isEmpty() || w.any { !it.isLetter() && it != '-' }) return
        val current = runCatching {
            prefs.getStringSet(USER_WORDS_KEY, emptySet())
        }.getOrNull() ?: emptySet()
        val updated = HashSet(current)
        if (updated.add(w)) {
            prefs.edit().putStringSet(USER_WORDS_KEY, updated).apply()
        }
        runCatching { engineFuture.get().addToDictionary(w) }
    }

    companion object {
        private const val PREFS_NAME = "spellcheck_user_dict"
        private const val USER_WORDS_KEY = "user_words"
        private const val DICT_ASSET = "pt_br_dict.txt"
    }
}