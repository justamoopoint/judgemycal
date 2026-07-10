package com.judgemycal.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.judgemycal.app.R
import com.judgemycal.app.domain.LoggedMeal
import java.text.DateFormat
import java.util.Date

@Composable
fun HomeScreen(meals: List<LoggedMeal>) {
    val today = todayMeals(meals)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (today.isEmpty()) {
            Text(
                text = stringResource(R.string.home_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("home_empty"),
            )
        } else {
            // The daily total is a range too — sums of ranges, honestly.
            Text(
                text = stringResource(
                    R.string.home_total,
                    today.sumOf { it.kcal },
                    today.sumOf { it.kcalLow },
                    today.sumOf { it.kcalHigh },
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("home_total"),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(today.asReversed()) { meal -> MealRow(meal) }
            }
        }
    }
}

@Composable
private fun MealRow(meal: LoggedMeal) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(meal.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = DateFormat.getTimeInstance(DateFormat.SHORT)
                        .format(Date(meal.timestampMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "~${meal.kcal} (${meal.kcalLow}–${meal.kcalHigh})",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun todayMeals(meals: List<LoggedMeal>): List<LoggedMeal> {
    val dayStart = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    return meals.filter { it.timestampMillis >= dayStart }
}
