package com.judgemycal.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.judgemycal.app.domain.LoggedMeal
import com.judgemycal.app.domain.Persona
import com.judgemycal.app.domain.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "judgemycal")

/**
 * Local persistence: the meal log and workout history (persona-agnostic memory —
 * switching companion never changes the data), the chosen persona, and the
 * backend session id (so agent memory continues across app opens).
 */
class MealRepository(context: Context) : BackendAgentService.SessionStore {

    private val store = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    private val mealsKey = stringPreferencesKey("meal_log")
    private val workoutsKey = stringPreferencesKey("workout_history")
    private val personaKey = stringPreferencesKey("persona")
    private val sessionKey = stringPreferencesKey("agent_session_id")

    val meals: Flow<List<LoggedMeal>> = store.data.map { prefs ->
        prefs[mealsKey]?.let { runCatching { json.decodeFromString<List<LoggedMeal>>(it) }.getOrNull() }
            ?: emptyList()
    }

    val workouts: Flow<List<WorkoutSession>> = store.data.map { prefs ->
        prefs[workoutsKey]?.let { runCatching { json.decodeFromString<List<WorkoutSession>>(it) }.getOrNull() }
            ?: emptyList()
    }

    val persona: Flow<Persona> = store.data.map { prefs ->
        Persona.fromKey(prefs[personaKey])
    }

    suspend fun addMeal(meal: LoggedMeal) {
        store.edit { prefs ->
            val current = prefs[mealsKey]
                ?.let { runCatching { json.decodeFromString<List<LoggedMeal>>(it) }.getOrNull() }
                ?: emptyList()
            prefs[mealsKey] = json.encodeToString(current + meal)
        }
    }

    suspend fun addWorkout(session: WorkoutSession) {
        store.edit { prefs ->
            val current = prefs[workoutsKey]
                ?.let { runCatching { json.decodeFromString<List<WorkoutSession>>(it) }.getOrNull() }
                ?: emptyList()
            // Keep the local history bounded; the backend session holds long-term memory.
            prefs[workoutsKey] = json.encodeToString((current + session).takeLast(20))
        }
    }

    suspend fun setPersona(persona: Persona) {
        store.edit { it[personaKey] = persona.key }
    }

    override suspend fun getSessionId(): String? = store.data.first()[sessionKey]

    override suspend fun setSessionId(id: String?) {
        store.edit { prefs ->
            if (id == null) prefs.remove(sessionKey) else prefs[sessionKey] = id
        }
    }
}
