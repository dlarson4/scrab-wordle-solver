package com.scrabble.solver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ScrabbleScreenContainer(
    viewModel: ScrabbleWordleViewModel = hiltViewModel(),
) {
    val results     by viewModel.results.collectAsStateWithLifecycle()
    val hasSearched by viewModel.hasSearched.collectAsStateWithLifecycle()
    val loadState   by viewModel.loadState.collectAsStateWithLifecycle()

    if (loadState is ScrabbleWordleViewModel.LoadState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text("Loading word list…", style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        ScrabbleWordleScreen(
            results     = results,
            hasSearched = hasSearched,
            onSolve     = viewModel::solve,
            onReset     = viewModel::reset,
        )
    }
}
