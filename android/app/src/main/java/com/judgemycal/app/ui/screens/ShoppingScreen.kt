package com.judgemycal.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.judgemycal.app.R
import com.judgemycal.app.domain.SwapResult

/**
 * v1 shows swap suggestions only — no purchasing, ever (spec §5). The agent may
 * prepare ideas; checkout is a different product.
 */
@Composable
fun ShoppingScreen(
    basket: List<String>,
    swaps: SwapResult?,
    busy: Boolean,
    onAddItem: (String) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSuggestSwaps: () -> Unit,
) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.shopping_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.shopping_add_hint)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("basket_input"),
            )
            Button(
                onClick = {
                    onAddItem(input)
                    input = ""
                },
                modifier = Modifier.testTag("basket_add"),
            ) { Text(stringResource(R.string.shopping_add)) }
        }

        if (basket.isEmpty()) {
            Text(
                text = stringResource(R.string.shopping_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                basket.forEachIndexed { index, item ->
                    InputChip(
                        selected = false,
                        onClick = { onRemoveItem(index) },
                        label = { Text(item) },
                    )
                }
            }
            if (busy) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = onSuggestSwaps,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("suggest_swaps"),
                ) { Text(stringResource(R.string.shopping_get_swaps)) }
            }
        }

        swaps?.let { result ->
            if (result.fromFallback) {
                AssistChip(onClick = {}, label = { Text(stringResource(R.string.estimate_offline_badge)) })
            }
            result.swaps.forEach { swap ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(swap.item, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = swap.swap,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("swap_suggestion"),
                        )
                    }
                }
            }
            if (result.swaps.isEmpty()) {
                Text(
                    text = "Your basket already looks aligned with your goals — nothing to suggest.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (result.personaReaction.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Text(
                        text = result.personaReaction,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.shopping_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
