package com.judgemycal.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.judgemycal.app.AppGraph
import com.judgemycal.app.data.AgentService
import com.judgemycal.app.data.MealRepository
import com.judgemycal.app.domain.CalorieEstimator
import com.judgemycal.app.domain.LoggedMeal
import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.domain.Persona
import com.judgemycal.app.domain.SafetyGuard
import com.judgemycal.app.domain.SwapResult
import com.judgemycal.app.domain.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One view model for the whole app: a single persona and a single agent
 * session shared across all three capabilities is the product's core promise —
 * same companion, same memory, different capability.
 *
 * Safety: every piece of user free text passes [SafetyGuard] BEFORE anything
 * else happens. On a distress signal, nothing is sent anywhere and the
 * support message takes over the screen — the client-side twin of the
 * backend's non-overridable before-model callback.
 */
class AppViewModel(
    private val agent: AgentService = AppGraph.agentService,
    private val store: MealRepository = AppGraph.mealRepository,
) : ViewModel() {

    val persona: StateFlow<Persona> = store.persona
        .stateIn(viewModelScope, SharingStarted.Eagerly, Persona.DEFAULT)

    val meals: StateFlow<List<LoggedMeal>> = store.meals
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val workoutHistory: StateFlow<List<WorkoutSession>> = store.workouts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _estimate = MutableStateFlow<MealEstimate?>(null)
    val estimate: StateFlow<MealEstimate?> = _estimate

    private val _swaps = MutableStateFlow<SwapResult?>(null)
    val swaps: StateFlow<SwapResult?> = _swaps

    private val _workout = MutableStateFlow<WorkoutSession?>(null)
    val workout: StateFlow<WorkoutSession?> = _workout

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    /** Non-null when the safety floor has broken character; owns the screen until dismissed. */
    private val _supportMessage = MutableStateFlow<String?>(null)
    val supportMessage: StateFlow<String?> = _supportMessage

    private val _basket = MutableStateFlow<List<String>>(emptyList())
    val basket: StateFlow<List<String>> = _basket

    fun setPersona(persona: Persona) {
        viewModelScope.launch { store.setPersona(persona) }
    }

    fun dismissSupportMessage() {
        _supportMessage.value = null
    }

    fun estimateMeal(imageJpeg: ByteArray?, note: String) {
        if (guarded(note)) return
        viewModelScope.launch {
            _busy.value = true
            try {
                _estimate.value = agent.estimateMeal(persona.value, imageJpeg, note)
            } finally {
                _busy.value = false
            }
        }
    }

    /** One-tap portion correction: recompute locally, keep the honest band. */
    fun correctPortion(itemName: String, grams: Int) {
        _estimate.value = _estimate.value?.let {
            CalorieEstimator.withPortion(it, itemName, grams)
        }
    }

    fun logMeal() {
        val est = _estimate.value ?: return
        viewModelScope.launch {
            store.addMeal(
                LoggedMeal(
                    name = est.items.firstOrNull()?.name ?: "meal",
                    kcal = est.totalKcal,
                    kcalLow = est.totalLow,
                    kcalHigh = est.totalHigh,
                    timestampMillis = System.currentTimeMillis(),
                ),
            )
            _estimate.value = null
        }
    }

    fun clearEstimate() {
        _estimate.value = null
    }

    fun addBasketItem(item: String) {
        if (guarded(item)) return
        val trimmed = item.trim()
        if (trimmed.isNotEmpty()) _basket.value = _basket.value + trimmed
    }

    fun removeBasketItem(index: Int) {
        _basket.value = _basket.value.filterIndexed { i, _ -> i != index }
    }

    fun suggestSwaps() {
        val items = _basket.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            _busy.value = true
            try {
                _swaps.value = agent.suggestSwaps(persona.value, items)
            } finally {
                _busy.value = false
            }
        }
    }

    fun buildWorkout(goal: String, minutes: Int, equipment: String) {
        if (guarded(goal) || guarded(equipment)) return
        viewModelScope.launch {
            _busy.value = true
            try {
                val session = agent.buildWorkout(persona.value, goal, minutes, equipment)
                _workout.value = session
                store.addWorkout(session)
            } finally {
                _busy.value = false
            }
        }
    }

    /** True when the text tripped the safety floor (and now owns the screen). */
    private fun guarded(text: String): Boolean {
        if (SafetyGuard.isDistress(text)) {
            _supportMessage.value = SafetyGuard.SUPPORT_MESSAGE
            return true
        }
        return false
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel() as T
        }
    }
}
