package com.judgemycal.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.judgemycal.app.R
import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.ui.components.ConfidenceBandCard
import com.judgemycal.app.ui.components.ConfidencePill
import java.io.ByteArrayOutputStream

@Composable
fun CaptureScreen(
    estimate: MealEstimate?,
    busy: Boolean,
    onEstimate: (imageJpeg: ByteArray?, note: String) -> Unit,
    onCorrectPortion: (itemName: String, grams: Int) -> Unit,
    onLogMeal: () -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    var note by remember { mutableStateOf("") }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        bitmap?.let { onEstimate(it.toJpegBytes(), note) }
    }
    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { picked ->
            context.contentResolver.openInputStream(picked)?.use { stream ->
                BitmapFactory.decodeStream(stream)?.let { onEstimate(it.toJpegBytes(), note) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.capture_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        if (estimate == null) {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.capture_note_hint)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input"),
            )
            if (busy) {
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            } else {
                Button(
                    onClick = { takePhoto.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("take_photo"),
                ) { Text(stringResource(R.string.capture_take_photo)) }
                OutlinedButton(
                    onClick = {
                        pickPhoto.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.capture_pick_photo)) }
                TextButton(
                    onClick = { onEstimate(null, note) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("demo_meal"),
                ) { Text(stringResource(R.string.capture_demo)) }
            }
        } else {
            EstimateResult(
                estimate = estimate,
                onCorrectPortion = onCorrectPortion,
                onLogMeal = { onLogMeal(); note = "" },
                onClear = onClear,
            )
        }
    }
}

@Composable
private fun EstimateResult(
    estimate: MealEstimate,
    onCorrectPortion: (String, Int) -> Unit,
    onLogMeal: () -> Unit,
    onClear: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (estimate.fromFallback) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.estimate_offline_badge)) },
                    modifier = Modifier.testTag("offline_badge"),
                )
            }
            ConfidenceBandCard(estimate)

            estimate.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${item.grams} g · ~${item.kcal} kcal " +
                                "(${item.kcalLow}–${item.kcalHigh})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ConfidencePill(item.confidence)
                }
                // One-tap portion correction: honest ranges invite correction.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.75, 1.25).forEach { factor ->
                        val grams = (item.grams * factor).toInt()
                        TextButton(
                            onClick = { onCorrectPortion(item.name, grams) },
                            modifier = Modifier.testTag("portion_${item.name}_$grams"),
                        ) {
                            Text(if (factor < 1) "Smaller ($grams g)" else "Bigger ($grams g)")
                        }
                    }
                }
            }

            if (estimate.personaReaction.isNotBlank()) {
                Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Text(
                        text = estimate.personaReaction,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .padding(12.dp)
                            .testTag("persona_reaction"),
                    )
                }
            }

            Button(
                onClick = onLogMeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("log_meal"),
            ) { Text(stringResource(R.string.estimate_log_meal)) }
            TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                Text("Start over")
            }
        }
    }
}

private fun Bitmap.toJpegBytes(maxDim: Int = 1280, quality: Int = 85): ByteArray {
    val scale = maxDim.toFloat() / maxOf(width, height)
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    } else {
        this
    }
    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }
}
