package com.scrabble.solver

import javax.inject.Inject

/** Standard English Scrabble letter values (lowercase keys). */
private val LETTER_VALUES: Map<Char, Int> = buildMap {
    "aeioulnstr".forEach { put(it, 1) }
    "dg".forEach      { put(it, 2) }
    "bcmp".forEach    { put(it, 3) }
    "fhvwy".forEach   { put(it, 4) }
    "k".forEach       { put(it, 5) }
    "jx".forEach      { put(it, 8) }
    "qz".forEach      { put(it, 10) }
}

data class ScrabWordleResult(val word: String, val score: Int)

class ScrabbleWordleSolver @Inject constructor() {

    /** Scrabble score for a word (non-alpha chars contribute 0). */
    fun scoreWord(word: String): Int =
        word.fold(0) { acc, ch -> acc + (LETTER_VALUES[ch.lowercaseChar()] ?: 0) }

    /**
     * Filters [words] according to ScrabbleWordle constraints:
     *  - Each position [exact] must match if non-empty
     *  - All [mustContain] letters must appear somewhere (independent of position)
     *  - No [excluded] letters may appear at all
     *  - Word length must equal [exact].size (5 for ScrabbleWordle)
     *
     * Results are sorted by word ascending and capped at [maxResults].
     */
    fun solve(
        words: List<String>,
        targetScore: Int,
        exact: List<String>,              // length 5; entries are "" or a single lowercase letter
        mustContain: Set<Char>,           // lowercase
        excluded: Set<Char>,              // lowercase
        positionExclusions: List<Set<Char>>, // per-position excluded letters (size == wordLen)
        wordLen: Int = 5,
        maxResults: Int = 200,
    ): List<ScrabWordleResult> {
        val exactChars = exact.map { it.firstOrNull()?.lowercaseChar() } // null if ""

        return words.asSequence()
            .filter { it.length == wordLen }
            .filter { word ->
                // Exact-position constraints
                exactChars.forEachIndexed { i, expected ->
                    if (expected != null && word[i] != expected) return@filter false
                }
                val letters = word.toSet()
                // Must-contain constraints
                if (!letters.containsAll(mustContain)) return@filter false
                // Global exclusion constraints
                if (letters.any { it in excluded }) return@filter false
                // Position-exclusion constraints
                word.forEachIndexed { i, ch ->
                    if (ch in positionExclusions[i]) return@filter false
                }
                true
            }
            .mapNotNull { word ->
                val s = scoreWord(word)
                if (targetScore == 0 || s == targetScore) ScrabWordleResult(word, s) else null
            }
            .sortedBy { it.word }
            .take(maxResults)
            .toList()
    }
}
