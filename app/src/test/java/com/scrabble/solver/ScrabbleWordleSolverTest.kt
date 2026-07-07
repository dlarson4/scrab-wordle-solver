package com.scrabble.solver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScrabbleWordleSolverTest {

    private lateinit var solver: ScrabbleWordleSolver

    // 5-letter words used across tests
    private val fiveLetterWords = listOf(
        "stare", "stamp", "stark", "stale", "stair", // sta**
        "arise", "raise",                              // anagrams of stare
        "jazzy", "fuzzy",                              // high-value z words
        "apple", "grape", "pearl",                     // misc
        "quirk", "quick", "quack",                     // q words
        "plate", "pleat",                              // anagrams
        "dream", "dread", "drank", "drama",            // d words
        "crane", "crank", "cramp", "crime",            // c words
    )

    private val allWords = fiveLetterWords + listOf(
        "cat", "dog", "be",          // wrong length (< 5)
        "longer", "shorter",          // wrong length (> 5)A
    )

    @Before
    fun setUp() {
        solver = ScrabbleWordleSolver()
    }

    // ──────────────────────────────────────────────────────────────
    //  scoreWord tests
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `scoreWord - empty string returns 0`() {
        assertEquals(0, solver.scoreWord(""))
    }

    @Test
    fun `scoreWord - mixed case is case insensitive`() {
        // "scrabble": s(1)+c(3)+r(1)+a(1)+b(3)+b(3)+l(1)+e(1) = 14
        assertEquals(14, solver.scoreWord("ScRaBbLe"))
    }

    @Test
    fun `scoreWord - all lowercase`() {
        assertEquals(14, solver.scoreWord("scrabble"))
    }

    @Test
    fun `scoreWord - non-alpha characters contribute 0`() {
        // "a1b!": a(1)+b(3) = 4
        assertEquals(4, solver.scoreWord("a1b!"))
    }

    @Test
    fun `scoreWord - all 1-point letters`() {
        // a,e,i,o,u,l,n,s,t,r are all 1-point
        assertEquals(7, solver.scoreWord("aeiouln"))
    }

    @Test
    fun `scoreWord - high value letters`() {
        // q=10, u=1, i=1, z=10, z=10 = 32
        assertEquals(32, solver.scoreWord("quizz"))
    }

    @Test
    fun `scoreWord - single letter each value`() {
        assertEquals(1, solver.scoreWord("a"))
        assertEquals(2, solver.scoreWord("d"))
        assertEquals(3, solver.scoreWord("b"))
        assertEquals(4, solver.scoreWord("f"))
        assertEquals(5, solver.scoreWord("k"))
        assertEquals(8, solver.scoreWord("j"))
        assertEquals(10, solver.scoreWord("q"))
        assertEquals(10, solver.scoreWord("z"))
    }

    // ──────────────────────────────────────────────────────────────
    //  solve - basic filtering
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `solve - empty word list returns empty`() {
        val result = solver.solve(
            words = emptyList(),
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `solve - filters by word length`() {
        val result = solver.solve(
            words = allWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        // Only 5-letter words should be returned
        result.forEach { assertEquals(5, it.word.length) }
        // Should not include "cat", "dog", "be", "longer", "shorter"
        val resultWords = result.map { it.word }
        assertFalse(resultWords.contains("cat"))
        assertFalse(resultWords.contains("longer"))
    }

    @Test
    fun `solve - targetScore 0 disables score filter`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        // Should return all 5-letter words since no score filter
        assertEquals(fiveLetterWords.size, result.size)
    }

    @Test
    fun `solve - filters by target score`() {
        // "stare", "stale", "stair", "arise", "raise" all score 5
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 5,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        result.forEach { assertEquals(5, it.score) }
        val resultWords = result.map { it.word }.toSet()
        assertTrue(resultWords.contains("stare"))
        assertTrue(resultWords.contains("arise"))
        assertTrue(resultWords.contains("raise"))
        // "jazzy" (33) should NOT be present
        assertFalse(resultWords.contains("jazzy"))
    }

    // ──────────────────────────────────────────────────────────────
    //  solve - exact position constraints
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `solve - exact position single match`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("s", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        result.forEach { assertEquals('s', it.word[0]) }
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `solve - exact position multiple matches`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("s", "", "", "", "e"),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        result.forEach {
            assertEquals('s', it.word[0])
            assertEquals('e', it.word[4])
        }
        // "stare" and "stale" match
        val resultWords = result.map { it.word }.toSet()
        assertTrue(resultWords.contains("stare"))
        assertTrue(resultWords.contains("stale"))
        // "stamp" does NOT end with 'e'
        assertFalse(resultWords.contains("stamp"))
    }

    @Test
    fun `solve - all 5 exact positions`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("s", "t", "a", "r", "e"),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        assertEquals(1, result.size)
        assertEquals("stare", result[0].word)
    }

    @Test
    fun `solve - exact position no match returns empty`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("z", "z", "z", "z", "z"),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        assertTrue(result.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────
    //  solve - mustContain constraints
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `solve - mustContain single letter`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = setOf('z'),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        result.forEach { assertTrue(it.word.contains('z')) }
        val resultWords = result.map { it.word }.toSet()
        assertTrue(resultWords.contains("jazzy"))
        assertTrue(resultWords.contains("fuzzy"))
        assertFalse(resultWords.contains("stare"))
    }

    @Test
    fun `solve - mustContain multiple letters`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = setOf('a', 'e'),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        result.forEach {
            assertTrue(it.word.contains('a'))
            assertTrue(it.word.contains('e'))
        }
        val resultWords = result.map { it.word }.toSet()
        assertTrue(resultWords.contains("stare"))
        assertTrue(resultWords.contains("stale"))
        // "stamp" has 'a' but no 'e'
        assertFalse(resultWords.contains("stamp"))
    }

    @Test
    fun `solve - mustContain with exact overlap`() {
        // 'e' is already covered by exact position 4, but mustContain also requires it
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", "e"),
            mustContain = setOf('e'),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        result.forEach {
            assertEquals('e', it.word[4])
            assertTrue(it.word.contains('e'))
        }
        assertTrue(result.isNotEmpty())
    }

    // ──────────────────────────────────────────────────────────────
    //  solve - excluded letters
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `solve - excluded letters globally`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = setOf('z', 'q'),
            positionExclusions = List(5) { emptySet() },
        )
        result.forEach {
            assertFalse(it.word.contains('z'))
            assertFalse(it.word.contains('q'))
        }
        val resultWords = result.map { it.word }.toSet()
        assertFalse(resultWords.contains("jazzy"))
        assertFalse(resultWords.contains("quirk"))
        assertTrue(resultWords.contains("stare"))
    }

    // ──────────────────────────────────────────────────────────────
    //  solve - position exclusions
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `solve - position exclusion single position`() {
        // Exclude 's' at position 0 — should filter out "stare", "stamp", etc.
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = listOf(
                setOf('s'), emptySet(), emptySet(), emptySet(), emptySet()
            ),
        )
        result.forEach { assertNotEquals('s', it.word[0]) }
        val resultWords = result.map { it.word }.toSet()
        assertFalse(resultWords.contains("stare"))
        assertTrue(resultWords.contains("apple"))
    }

    @Test
    fun `solve - position exclusion multiple positions`() {
        // Exclude 's' at 0 and 'e' at 4
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = listOf(
                setOf('s'), emptySet(), emptySet(), emptySet(), setOf('e')
            ),
        )
        result.forEach {
            assertNotEquals('s', it.word[0])
            assertNotEquals('e', it.word[4])
        }
        val resultWords = result.map { it.word }.toSet()
        assertFalse(resultWords.contains("stare"))
        assertFalse(resultWords.contains("apple"))
        assertTrue(resultWords.contains("pearl"))
        assertTrue(resultWords.contains("drama"))
    }

    @Test
    fun `solve - position exclusion vs global exclusion`() {
        // Global exclusion of 'e' blocks it everywhere
        val global = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = setOf('e'),
            positionExclusions = List(5) { emptySet() },
        )
        global.forEach { assertFalse(it.word.contains('e')) }

        // Position exclusion of 'e' only at index 2 leaves other positions free
        val pos = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = listOf(
                emptySet(), emptySet(), setOf('e'), emptySet(), emptySet()
            ),
        )
        pos.forEach { assertNotEquals('e', it.word[2]) }
        // "stare" has 'e' at 4, not 2 — should still be in the position-exclusion results
        val posWords = pos.map { it.word }.toSet()
        assertTrue(posWords.contains("stare"))
    }

    // ──────────────────────────────────────────────────────────────
    //  solve - edge cases
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `solve - all constraints empty returns all words of correct length`() {
        val result = solver.solve(
            words = allWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        assertEquals(fiveLetterWords.size, result.size)
        result.forEach { assertEquals(5, it.word.length) }
    }

    @Test
    fun `solve - respects maxResults cap`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
            maxResults = 3,
        )
        assertEquals(3, result.size)
    }

    @Test
    fun `solve - results sorted alphabetically`() {
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        val words = result.map { it.word }
        assertEquals(words.sorted(), words)
    }

    @Test
    fun `solve - contradictory constraints return empty`() {
        // exact requires 'a' at position 0, but excluded bans 'a' everywhere
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("a", "", "", "", ""),
            mustContain = emptySet(),
            excluded = setOf('a'),
            positionExclusions = List(5) { emptySet() },
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `solve - exact entries are lowercased`() {
        // exact entries get lowercased internally; mustContain/excluded expect lowercase
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("S", "", "", "", "E"),
            mustContain = setOf('a'),
            excluded = setOf('z'),
            positionExclusions = List(5) { emptySet() },
        )
        result.forEach {
            assertEquals('s', it.word[0])
            assertEquals('e', it.word[4])
            assertTrue(it.word.contains('a'))
        }
        val resultWords = result.map { it.word }.toSet()
        assertTrue(resultWords.contains("stare"))
        assertTrue(resultWords.contains("stale"))
    }

    // ──────────────────────────────────────────────────────────────
    //  solve - combined constraints
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `solve - combined exact, mustContain, excluded, and positionExclusions`() {
        // Find words that:
        //  - start with 's' (exact[0])
        //  - contain 'a' (mustContain)
        //  - do NOT contain 'z' (excluded)
        //  - do NOT have 'r' at position 3 (positionExclusions)
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 0,
            exact = listOf("s", "", "", "", ""),
            mustContain = setOf('a'),
            excluded = setOf('z'),
            positionExclusions = listOf(
                emptySet(), emptySet(), emptySet(), setOf('r'), emptySet()
            ),
        )
        result.forEach {
            assertEquals('s', it.word[0])
            assertTrue(it.word.contains('a'))
            assertFalse(it.word.contains('z'))
            assertNotEquals('r', it.word[3])
        }
        val resultWords = result.map { it.word }.toSet()
        // "stare" has 'r' at 3 → excluded
        assertFalse(resultWords.contains("stare"))
        // "stale" has 'l' at 3 → included
        assertTrue(resultWords.contains("stale"))
        // "stamp" has 'm' at 3 → included
        assertTrue(resultWords.contains("stamp"))
    }

    @Test
    fun `solve - combined with targetScore`() {
        // "stare" = 5, "stale" = 5, "stamp" = 9, "stark" = 9, "stair" = 5
        val result = solver.solve(
            words = fiveLetterWords,
            targetScore = 5,
            exact = listOf("s", "", "", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(5) { emptySet() },
        )
        result.forEach {
            assertEquals('s', it.word[0])
            assertEquals(5, it.score)
        }
        val resultWords = result.map { it.word }.toSet()
        assertTrue(resultWords.contains("stare"))
        assertTrue(resultWords.contains("stale"))
        // "stamp" and "stark" score 9, should be excluded
        assertFalse(resultWords.contains("stamp"))
        assertFalse(resultWords.contains("stark"))
    }

    @Test
    fun `solve - custom word length`() {
        // wordLen=3 should only match "cat" and "dog"
        val result = solver.solve(
            words = allWords,
            targetScore = 0,
            exact = listOf("", "", ""),
            mustContain = emptySet(),
            excluded = emptySet(),
            positionExclusions = List(3) { emptySet() },
            wordLen = 3,
        )
        result.forEach { assertEquals(3, it.word.length) }
        val resultWords = result.map { it.word }.toSet()
        assertTrue(resultWords.contains("cat"))
        assertTrue(resultWords.contains("dog"))
        assertFalse(resultWords.contains("be"))
    }
}