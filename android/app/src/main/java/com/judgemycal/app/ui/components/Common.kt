package com.judgemycal.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.judgemycal.app.domain.Confidence
import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.ui.ConfidenceHigh
import com.judgemycal.app.ui.ConfidenceLow
import com.judgemycal.app.ui.ConfidenceMedium

fun confidenceColor(confidence: Confidence): Color = when (confidence) {
    Confidence.HIGH -> ConfidenceHigh
    Confidence.MEDIUM -> ConfidenceMedium
    Confidence.LOW -> ConfidenceLow
}

/** Small pill labelling a confidence level — informative, never alarming. */
@Composable
fun ConfidencePill(confidence: Confidence, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = confidenceColor(confidence).copy(alpha = 0.18f),
        contentColor = confidenceColor(confidence),
    ) {
        Text(
            text = confidence.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/**
 * The signature visual: the calorie total drawn as a band from low to high,
 * with the point estimate inside it. The range IS the answer; the single
 * number is just where the middle of the band sits.
 */
@Composable
fun ConfidenceBandCard(estimate: MealEstimate, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "~${estimate.totalKcal} kcal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.testTag("estimate_total"),
        )
        Text(
            text = "${estimate.totalLow}–${estimate.totalHigh} kcal",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("estimate_range"),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val span = (estimate.totalHigh - estimate.totalLow).coerceAtLeast(1)
            val pointFraction = (estimate.totalKcal - estimate.totalLow).toFloat() / span
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (pointFraction > 0f) {
                    Surface(
                        color = confidenceColor(estimate.overallConfidence).copy(alpha = 0.45f),
                        modifier = Modifier
                            .weight(pointFraction.coerceAtLeast(0.01f))
                            .height(10.dp),
                    ) {}
                }
                Surface(
                    color = confidenceColor(estimate.overallConfidence),
                    modifier = Modifier
                        .width(6.dp)
                        .height(10.dp),
                ) {}
                if (pointFraction < 1f) {
                    Surface(
                        color = confidenceColor(estimate.overallConfidence).copy(alpha = 0.45f),
                        modifier = Modifier
                            .weight((1f - pointFraction).coerceAtLeast(0.01f))
                            .height(10.dp),
                    ) {}
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConfidencePill(estimate.overallConfidence)
            estimate.lowestConfidenceItem?.let {
                Text(
                    text = "least sure about: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The break-character moment. Calm, plain, unmissable — no persona styling,
 * because the persona has deliberately stepped aside.
 */
@Composable
fun SupportMessageDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Before anything else —") },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("support_message"),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("support_dismiss")) {
                Text("Okay")
            }
        },
    )
}
