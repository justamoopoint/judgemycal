package com.judgemycal.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.judgemycal.app.R
import com.judgemycal.app.domain.Persona
import com.judgemycal.app.ui.components.SupportMessageDialog
import com.judgemycal.app.ui.screens.CaptureScreen
import com.judgemycal.app.ui.screens.HomeScreen
import com.judgemycal.app.ui.screens.ShoppingScreen
import com.judgemycal.app.ui.screens.WorkoutScreen

private enum class Tab(val route: String, val labelRes: Int) {
    HOME("home", R.string.nav_home),
    CAPTURE("capture", R.string.nav_capture),
    SHOPPING("shopping", R.string.nav_shopping),
    WORKOUT("workout", R.string.nav_workout),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JudgeMyCalRoot(viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)) {
    val navController = rememberNavController()
    val persona by viewModel.persona.collectAsState()
    val supportMessage by viewModel.supportMessage.collectAsState()
    var showPersonaSheet by remember { mutableStateOf(false) }

    // The safety floor owns the screen when triggered — everything else waits.
    supportMessage?.let { message ->
        SupportMessageDialog(message = message, onDismiss = viewModel::dismissSupportMessage)
    }

    if (showPersonaSheet) {
        PersonaSheet(
            current = persona,
            onPick = {
                viewModel.setPersona(it)
                showPersonaSheet = false
            },
            onDismiss = { showPersonaSheet = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(
                        onClick = { showPersonaSheet = true },
                        modifier = Modifier.testTag("persona_switcher"),
                    ) {
                        Text(personaLabel(persona))
                    }
                },
            )
        },
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val currentRoute = backStack?.destination?.route
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {},
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                modifier = Modifier.testTag("nav_${tab.route}"),
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.HOME.route) {
                val meals by viewModel.meals.collectAsState()
                HomeScreen(meals = meals)
            }
            composable(Tab.CAPTURE.route) {
                val estimate by viewModel.estimate.collectAsState()
                val busy by viewModel.busy.collectAsState()
                CaptureScreen(
                    estimate = estimate,
                    busy = busy,
                    onEstimate = viewModel::estimateMeal,
                    onCorrectPortion = viewModel::correctPortion,
                    onLogMeal = viewModel::logMeal,
                    onClear = viewModel::clearEstimate,
                )
            }
            composable(Tab.SHOPPING.route) {
                val basket by viewModel.basket.collectAsState()
                val swaps by viewModel.swaps.collectAsState()
                val busy by viewModel.busy.collectAsState()
                ShoppingScreen(
                    basket = basket,
                    swaps = swaps,
                    busy = busy,
                    onAddItem = viewModel::addBasketItem,
                    onRemoveItem = viewModel::removeBasketItem,
                    onSuggestSwaps = viewModel::suggestSwaps,
                )
            }
            composable(Tab.WORKOUT.route) {
                val workout by viewModel.workout.collectAsState()
                val history by viewModel.workoutHistory.collectAsState()
                val busy by viewModel.busy.collectAsState()
                WorkoutScreen(
                    workout = workout,
                    history = history,
                    busy = busy,
                    onBuild = viewModel::buildWorkout,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonaSheet(
    current: Persona,
    onPick: (Persona) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.persona_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            // Buddy first: it's the default; Auntie/Coach are opt-in.
            listOf(
                Triple(Persona.BUDDY, R.string.persona_buddy, R.string.persona_buddy_desc),
                Triple(Persona.AUNTIE, R.string.persona_auntie, R.string.persona_auntie_desc),
                Triple(Persona.COACH, R.string.persona_coach, R.string.persona_coach_desc),
            ).forEach { (persona, nameRes, descRes) ->
                Card(
                    onClick = { onPick(persona) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (persona == current) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("persona_${persona.key}"),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(nameRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(descRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.settings_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun personaLabel(persona: Persona): String = when (persona) {
    Persona.BUDDY -> "Buddy"
    Persona.AUNTIE -> "Auntie"
    Persona.COACH -> "Coach"
}
