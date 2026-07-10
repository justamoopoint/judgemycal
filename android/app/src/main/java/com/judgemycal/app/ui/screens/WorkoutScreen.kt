package com.judgemycal.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.judgemycal.app.domain.WorkoutSession

@Composable
fun WorkoutScreen(
    workout: WorkoutSession?,
    history: List<WorkoutSession>,
    busy: Boolean,
    onBuild: (goal: String, minutes: Int, equipment: String) -> Unit,
) {
    var goal by remember { mutableStateOf("general fitness") }
    var minutes by remember { mutableFloatStateOf(30f) }
    var equipment by remember { mutableStateOf("none") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.workout_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = goal,
            onValueChange = { goal = it },
            label = { Text(stringResource(R.string.workout_goal_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("workout_goal"),
        )
        Text(stringResource(R.string.workout_minutes, minutes.toInt()))
        Slider(
            value = minutes,
            onValueChange = { minutes = it },
            valueRange = 10f..90f,
        )
        OutlinedTextField(
            value = equipment,
            onValueChange = { equipment = it },
            label = { Text(stringResource(R.string.workout_equipment_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )

        if (busy) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        } else {
            Button(
                onClick = { onBuild(goal, minutes.toInt(), equipment) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("workout_build"),
            ) { Text(stringResource(R.string.workout_build)) }
        }

        workout?.let { session ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (session.fromFallback) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.estimate_offline_badge)) },
                        )
                    }
                    Text(
                        text = "${session.goal} · ${session.totalMinutes} min",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("workout_session"),
                    )
                    Text(stringResource(R.string.workout_warmup, session.warmupMin))
                    Text(stringResource(R.string.workout_main, session.mainMin))
                    session.mainBlock.forEach { exercise ->
                        Text(
                            text = "  • $exercise",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(stringResource(R.string.workout_cooldown, session.cooldownMin))
                    Text(
                        text = session.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (session.personaReaction.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        ) {
                            Text(
                                text = session.personaReaction,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            Text(
                text = stringResource(R.string.workout_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            history.asReversed().take(5).forEach { past ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${past.goal} · ${past.totalMinutes} min · ${past.equipment}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}
