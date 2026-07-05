package com.scrabble.solver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScrabbleWordleViewModel @Inject constructor(
    private val solver: ScrabbleWordleSolver,
    private val wordListLoader: WordListLoader,
) : ViewModel() {

    // ── Word list (loaded off the main thread) ────────────────────────
    private val _wordList = MutableStateFlow<List<String>>(emptyList())

    private val _loadState = MutableStateFlow<LoadState>(LoadState.Loading)
    val loadState: StateFlow<LoadState> = _loadState.asStateFlow()

    // ── Results (observed by UI) ──────────────────────────────────────
    private val _results = MutableStateFlow<List<ScrabWordleResult>>(emptyList())
    val results: StateFlow<List<ScrabWordleResult>> = _results.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val words = wordListLoader.load()
            _wordList.value = words
            _loadState.value = LoadState.Ready
        }
    }

    // ── Public actions (called by Activity, passed as lambdas to UI) ──

    fun solve(
        targetScore: Int,
        exact: List<String>,
        mustContain: Set<Char>,
        excluded: Set<Char>,
        positionExclusions: List<Set<Char>>,
    ) {
        val words = _wordList.value.ifEmpty { return }
        viewModelScope.launch(Dispatchers.Default) {
            val solved = solver.solve(words, targetScore, exact, mustContain, excluded, positionExclusions)
            _results.value = solved
            _hasSearched.value = true
        }
    }

    fun reset() {
        _results.value = emptyList()
        _hasSearched.value = false
    }

    sealed class LoadState {
        object Loading : LoadState()
        object Ready : LoadState()
    }
}
