package com.scrabble.solver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.scrabble.solver.ui.theme.MyApplicationTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScrabbleWordleScreen(
    results: List<ScrabWordleResult>,
    hasSearched: Boolean,
    onSolve: (targetScore: Int, exact: List<String>, mustContain: Set<Char>, excluded: Set<Char>, positionExclusions: List<Set<Char>>) -> Unit,
    onReset: () -> Unit,
) {
    // ── Transient UI state (purely local) ─────────────────────────────
    var targetScore by rememberSaveable { mutableStateOf("") }
    val exactPositions = remember { mutableStateListOf("", "", "", "", "") }
    val mustContain = remember { mutableStateListOf<Char>() }
    val excluded = remember { mutableStateListOf<Char>() }
    var selectedMustLetter by remember { mutableStateOf<Char?>(null) }
    val letterPositionExclusions = remember { mutableStateMapOf<Char, Set<Int>>() }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Target Score ──────────────────────────────────────
        Text(
            text = "ScrabWordle Solver",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Target Score:",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = targetScore,
                    onValueChange = { value ->
                        // Only digits, max 2 chars
                        val filtered = value.filter { it.isDigit() }.take(2)
                        targetScore = filtered
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.width(80.dp),
                )
            }
        }

        // ── Exact Positions ───────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Exact positions (green tiles)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    for (i in 0 until 5) {
                        OutlinedTextField(
                            value = exactPositions[i].uppercase(),
                            onValueChange = { value ->
                                val filtered = value
                                    .lowercase()
                                    .filter { it.isLetter() }
                                    .take(1)
                                exactPositions[i] = filtered
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                textAlign = TextAlign.Center,
                            ),
                            modifier = Modifier.width(52.dp),
                        )
                    }
                }
                Text(
                    text = "Leave blank for unknown positions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Must Contain + Position Assignment ─────────────────
        MustContainSection(
            mustContain = mustContain,
            excluded = excluded,
            selectedMustLetter = selectedMustLetter,
            letterPositionExclusions = letterPositionExclusions,
            onSelectLetter = { ch ->
                selectedMustLetter = if (selectedMustLetter == ch) null else ch
            },
            onAddLetter = { ch -> mustContain.add(ch) },
            onRemoveLetter = { ch ->
                mustContain.remove(ch)
                letterPositionExclusions.remove(ch)
                if (selectedMustLetter == ch) selectedMustLetter = null
            },
            onTogglePosition = { pos ->
                val ch = selectedMustLetter ?: return@MustContainSection
                val current = letterPositionExclusions[ch].orEmpty()
                letterPositionExclusions[ch] =
                    if (pos in current) current - pos else current + pos
            },
        )

        // ── Excluded ──────────────────────────────────────────
        LetterChipSection(
            title = "Excluded (gray tiles)",
            letters = excluded,
            availableLetters = ('a'..'z').toSet() - mustContain.toSet() - excluded.toSet(),
            onAdd = { ch -> excluded.add(ch) },
            onRemove = { ch -> excluded.remove(ch) },
            chipColor = MaterialTheme.colorScheme.errorContainer,
            onChipColor = MaterialTheme.colorScheme.onErrorContainer,
        )


        // ── Action Buttons ─────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = {
                    val score = targetScore.toIntOrNull() ?: 0
                    // Convert per-letter Map<Int> → per-position List<Set<Char>>
                    val posExcl = List(5) { pos ->
                        letterPositionExclusions
                            .filter { pos in it.value }
                            .keys
                    }
                    onSolve(score, exactPositions.toList(), mustContain.toSet(), excluded.toSet(), posExcl)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("🔍  Solve", style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = {
                    targetScore = ""
                    for (i in 0 until 5) exactPositions[i] = ""
                    mustContain.clear()
                    excluded.clear()
                    selectedMustLetter = null
                    letterPositionExclusions.clear()
                    onReset()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("↺  Reset")
            }
        }

        // ── Results ───────────────────────────────────────────
        if (hasSearched) {
            HorizontalDivider()
            if (results.isEmpty()) {
                Text(
                    text = "No words found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Results (${results.size}):",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        results.forEach { result ->
                            ResultRow(result)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Sub-components ──────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LetterChipSection(
    title: String,
    letters: MutableList<Char>,
    availableLetters: Set<Char>,
    onAdd: (Char) -> Unit,
    onRemove: (Char) -> Unit,
    chipColor: Color,
    onChipColor: Color,
) {
    var selectedLetter by remember { mutableStateOf<Char?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            // Show current selections as removable chips
            if (letters.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    letters.sorted().forEach { ch ->
                        AssistChip(
                            onClick = { onRemove(ch) },
                            label = { Text(ch.uppercaseChar().toString(), fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = chipColor,
                                labelColor = onChipColor,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Letter picker: tap a letter to add it
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ('a'..'z').forEach { ch ->
                    val isAvailable = ch in availableLetters
                    val isSelected = ch == selectedLetter
                    AssistChip(
                        onClick = {
                            if (isSelected) {
                                // Second tap adds it
                                if (isAvailable) {
                                    onAdd(ch)
                                }
                                selectedLetter = null
                            } else {
                                selectedLetter = ch
                            }
                        },
                        enabled = isAvailable || ch in letters,
                        label = {
                            Text(
                                ch.uppercaseChar().toString(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when {
                                ch in letters -> chipColor
                                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            labelColor = when {
                                ch in letters -> onChipColor
                                isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    )
                }
            }
            Text(
                text = "Tap letter once to select, again to add. Tap a chip above to remove.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultRow(result: ScrabWordleResult) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = result.word.uppercase(),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "${result.score}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ─── Must-Contain + Position Assignment ──────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MustContainSection(
    mustContain: MutableList<Char>,
    excluded: MutableList<Char>,
    selectedMustLetter: Char?,
    letterPositionExclusions: Map<Char, Set<Int>>,
    onSelectLetter: (Char) -> Unit,
    onAddLetter: (Char) -> Unit,
    onRemoveLetter: (Char) -> Unit,
    onTogglePosition: (Int) -> Unit,
) {
    var pickerSelected by remember { mutableStateOf<Char?>(null) }
    val availableLetters = ('a'..'z').toSet() - mustContain.toSet() - excluded.toSet()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "In word, wrong position (yellow tiles)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            // ── Must-contain chips (tap to select for position assignment) ──
            if (mustContain.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    mustContain.sorted().forEach { ch ->
                        val isSelected = ch == selectedMustLetter
                        val excludedCount = letterPositionExclusions[ch]?.size ?: 0
                        val chipLabel = buildString {
                            append(ch.uppercaseChar())
                            if (excludedCount > 0) append(" ($excludedCount)")
                        }
                        AssistChip(
                            onClick = { onSelectLetter(ch) },
                            label = {
                                Text(
                                    chipLabel,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.secondaryContainer
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = if (isSelected)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Position toggle row (when a letter is selected) ──
            if (selectedMustLetter != null) {
                PositionToggleRow(
                    letter = selectedMustLetter,
                    excludedPositions = letterPositionExclusions[selectedMustLetter].orEmpty(),
                    onTogglePosition = onTogglePosition,
                    onRemove = { onRemoveLetter(selectedMustLetter) },
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Alphabet picker ──────────────────────────────
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ('a'..'z').forEach { ch ->
                    val isAvailable = ch in availableLetters
                    val isPickerSelected = ch == pickerSelected
                    AssistChip(
                        onClick = {
                            if (isPickerSelected) {
                                if (isAvailable) onAddLetter(ch)
                                pickerSelected = null
                            } else {
                                pickerSelected = ch
                            }
                        },
                        enabled = isAvailable || ch in mustContain,
                        label = {
                            Text(
                                ch.uppercaseChar().toString(),
                                fontWeight = if (isPickerSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when {
                                ch in mustContain -> MaterialTheme.colorScheme.tertiaryContainer
                                isPickerSelected -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            labelColor = when {
                                ch in mustContain -> MaterialTheme.colorScheme.onTertiaryContainer
                                isPickerSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    )
                }
            }
            Text(
                text = if (selectedMustLetter != null)
                    "Toggle positions ${selectedMustLetter.uppercaseChar()} can't be in. Tap ✕ to remove letter."
                else
                    "Tap alphabet to add. Tap a yellow chip above to assign positions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PositionToggleRow(
    letter: Char,
    excludedPositions: Set<Int>,
    onTogglePosition: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Excluded positions for ${letter.uppercaseChar()}:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until 5) {
                    FilterChip(
                        selected = i in excludedPositions,
                        onClick = { onTogglePosition(i) },
                        label = { Text("${i + 1}", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.width(48.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick = onRemove,
                    label = { Text("✕ Remove", color = MaterialTheme.colorScheme.error) },
                )
            }
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ScrabbleWordleScreenPreview() {
    MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ScrabbleWordleScreen(
                results     = listOf(ScrabWordleResult("arise", 5)),
                hasSearched = true,
                onSolve     = { _, _, _, _, _ -> },
                onReset     = {},
            )
        }
    }
}
