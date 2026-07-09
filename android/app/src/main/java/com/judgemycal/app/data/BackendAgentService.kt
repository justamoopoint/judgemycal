package com.judgemycal.app.data

import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.domain.Persona
import com.judgemycal.app.domain.SwapResult
import com.judgemycal.app.domain.WorkoutSession
import java.io.IOException
import java.util.Base64
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException

/** Raised when the agent answered but without the structured tool result the UI needs. */
class NoStructuredResultException(toolName: String) :
    IOException("Agent response contained no $toolName result")

/**
 * Talks to the deployed agent over the ADK wire API. One persona + one session
 * shared across all three capabilities: the session id is persisted locally so
 * backend memory ("last time you skipped Tuesday") survives app restarts, and
 * the active persona rides along as a stateDelta on every run.
 */
class BackendAgentService(
    private val api: AgentApi,
    private val sessionStore: SessionStore,
    /** Returns the signed-in Firebase UID, signing in anonymously if needed. */
    private val uidProvider: suspend () -> String,
    private val appName: String = "judgemycal",
) : AgentService {

    interface SessionStore {
        suspend fun getSessionId(): String?
        suspend fun setSessionId(id: String?)
    }

    override suspend fun estimateMeal(
        persona: Persona,
        imageJpeg: ByteArray?,
        note: String,
    ): MealEstimate {
        val parts = buildList {
            val prompt = buildString {
                append("Please estimate the calories in this meal photo.")
                if (note.isNotBlank()) append(" Note from me: $note")
            }
            add(WirePart(text = prompt))
            if (imageJpeg != null) {
                add(
                    WirePart(
                        inlineData = WireBlob(
                            mimeType = "image/jpeg",
                            data = Base64.getEncoder().encodeToString(imageJpeg),
                        ),
                    ),
                )
            }
        }
        val events = run(persona, parts)
        val structured = AgentResponseMapper.functionResponse(events, "estimate_meal")
            ?: throw NoStructuredResultException("estimate_meal")
        return AgentResponseMapper.mealEstimate(structured, AgentResponseMapper.finalText(events))
    }

    override suspend fun suggestSwaps(persona: Persona, items: List<String>): SwapResult {
        val prompt = "Here's my shopping basket: ${items.joinToString(", ")}. " +
            "Any goal-friendly swaps you'd suggest?"
        val events = run(persona, listOf(WirePart(text = prompt)))
        val structured = AgentResponseMapper.functionResponse(events, "suggest_swaps")
            ?: throw NoStructuredResultException("suggest_swaps")
        return AgentResponseMapper.swapResult(structured, AgentResponseMapper.finalText(events))
    }

    override suspend fun buildWorkout(
        persona: Persona,
        goal: String,
        minutes: Int,
        equipment: String,
    ): WorkoutSession {
        val prompt = "Build me a workout session. Goal: $goal. " +
            "I have $minutes minutes. Equipment: $equipment."
        val events = run(persona, listOf(WirePart(text = prompt)))
        val structured = AgentResponseMapper.functionResponse(events, "build_session")
            ?: throw NoStructuredResultException("build_session")
        return AgentResponseMapper.workoutSession(structured, AgentResponseMapper.finalText(events))
    }

    private suspend fun run(persona: Persona, parts: List<WirePart>): List<WireEvent> {
        val uid = uidProvider()
        val sessionId = ensureSession(uid, persona)
        val request = RunAgentRequest(
            appName = appName,
            userId = uid,
            sessionId = sessionId,
            newMessage = WireContent(role = "user", parts = parts),
            stateDelta = JsonObject(mapOf("persona" to JsonPrimitive(persona.key))),
        )
        return try {
            api.run(request)
        } catch (e: HttpException) {
            // The stored session can vanish (backend redeploy without persistent
            // sessions): recreate once and retry rather than dying.
            if (e.code() != 404) throw e
            sessionStore.setSessionId(null)
            api.run(request.copy(sessionId = ensureSession(uid, persona)))
        }
    }

    private suspend fun ensureSession(uid: String, persona: Persona): String {
        sessionStore.getSessionId()?.let { existing ->
            try {
                return api.getSession(appName, uid, existing).id
            } catch (e: HttpException) {
                if (e.code() != 404) throw e
                sessionStore.setSessionId(null)
            }
        }
        val created = api.createSession(
            appName, uid,
            CreateSessionRequest(
                state = JsonObject(mapOf("persona" to JsonPrimitive(persona.key))),
            ),
        )
        sessionStore.setSessionId(created.id)
        return created.id
    }
}
