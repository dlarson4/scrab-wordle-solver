package com.scrabble.solver

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WordListLoader @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "WordListLoader"
        private const val ASSET_FILE = "five_letter_words_lower.txt"

        val FALLBACK_WORDS = listOf(
            "arise", "crane", "stare", "oecia", "jazzy",
            "hello", "world", "table", "react", "trace",
        )
    }

    fun load(): List<String> = try {
        context.assets
            .open(ASSET_FILE)
            .bufferedReader()
            .useLines { lines ->
                lines.mapNotNull { line ->
                    val trimmed = line.trim()
                    if (trimmed.length == 5 && trimmed.all { it.isLetter() }) {
                        trimmed.lowercase()
                    } else {
                        null
                    }
                }.toList()
            }
            .also { Log.d(TAG, "Loaded ${it.size} words from $ASSET_FILE") }
    } catch (e: Exception) {
        Log.e(TAG, "Error loading word file, using fallback", e)
        FALLBACK_WORDS
    }
}
