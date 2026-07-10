package com.judgemycal.app.data

import com.judgemycal.app.domain.MealEstimate
import com.judgemycal.app.domain.Persona
import com.judgemycal.app.domain.SwapResult
import com.judgemycal.app.domain.WorkoutSession
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class InMemorySessionStore : BackendAgentService.SessionStore {
    var id: String? = null
    override suspend fun getSessionId(): String? = id
    override suspend fun setSessionId(id: String?) {
        this.id = id
    }
}

class BackendAgentServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: BackendAgentService
    private lateinit var sessionStore: InMemorySessionStore

    private val runEventsBody = """
    [
      {"id": "e1", "author": "cv_calorie", "content": {"role": "user", "parts": [
        {"functionResponse": {"id": "f1", "name": "estimate_meal", "response": {
          "items": [{"name": "banana", "grams": 120, "kcal": 107, "kcal_low": 96,
                     "kcal_high": 118, "protein": 1.3, "carbs": 27.6, "fat": 0.4,
                     "confidence": "HIGH"}],
          "total_kcal": 107, "total_low": 96, "total_high": 118,
          "protein": 1.3, "carbs": 27.6, "fat": 0.4,
          "overall_confidence": "HIGH", "lowest_confidence_item": null, "note": ""
        }}}]}},
      {"id": "e2", "author": "judgemycal", "content": {"role": "model", "parts": [
        {"text": "Nice! ~107 kcal (96-118)."}]}}
    ]
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        sessionStore = InMemorySessionStore()
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AgentApi::class.java)
        service = BackendAgentService(api, sessionStore, uidProvider = { "uid-1" })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `creates a session bound to the firebase uid and parses the estimate`() = runTest {
        server.enqueue(json("""{"id": "sess-1", "appName": "judgemycal", "userId": "uid-1"}"""))
        server.enqueue(json(runEventsBody))

        val estimate: MealEstimate = service.estimateMeal(Persona.BUDDY, null, "no oil")

        assertEquals(107, estimate.totalKcal)
        assertEquals(96, estimate.totalLow)
        assertEquals("sess-1", sessionStore.id)

        val createRequest = server.takeRequest()
        assertEquals("/apps/judgemycal/users/uid-1/sessions", createRequest.path)
        assertTrue(createRequest.body.readUtf8().contains("\"persona\":\"buddy\""))

        val runRequest = server.takeRequest()
        assertEquals("/run", runRequest.path)
        val body = runRequest.body.readUtf8()
        assertTrue(body.contains("\"userId\":\"uid-1\""))
        assertTrue(body.contains("\"sessionId\":\"sess-1\""))
        assertTrue(body.contains("no oil"))
        // Persona rides along on every run as a stateDelta.
        assertTrue(body.contains("\"stateDelta\":{\"persona\":\"buddy\"}"))
    }

    @Test
    fun `reuses a stored session across calls`() = runTest {
        sessionStore.id = "sess-existing"
        server.enqueue(json("""{"id": "sess-existing", "appName": "judgemycal", "userId": "uid-1"}"""))
        server.enqueue(json(runEventsBody))

        service.estimateMeal(Persona.COACH, null, "")

        assertEquals("/apps/judgemycal/users/uid-1/sessions/sess-existing", server.takeRequest().path)
        assertEquals("/run", server.takeRequest().path)
    }

    @Test
    fun `recreates the session when the stored one is gone`() = runTest {
        sessionStore.id = "sess-stale"
        server.enqueue(MockResponse().setResponseCode(404)) // GET stale session
        server.enqueue(json("""{"id": "sess-new", "appName": "judgemycal", "userId": "uid-1"}"""))
        server.enqueue(json(runEventsBody))

        service.estimateMeal(Persona.BUDDY, null, "")
        assertEquals("sess-new", sessionStore.id)
    }

    private fun json(body: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(body)
}

class CompositeAgentServiceTest {

    private object FailingBackend : AgentService {
        override suspend fun estimateMeal(persona: Persona, imageJpeg: ByteArray?, note: String): MealEstimate =
            throw IOException("network down")

        override suspend fun suggestSwaps(persona: Persona, items: List<String>): SwapResult =
            throw IOException("network down")

        override suspend fun buildWorkout(persona: Persona, goal: String, minutes: Int, equipment: String): WorkoutSession =
            throw IOException("network down")
    }

    @Test
    fun `backend failure degrades to flagged on-device fallback, not a dead screen`() = runTest {
        val composite = CompositeAgentService(FailingBackend, FallbackAgentService())

        val estimate = composite.estimateMeal(Persona.BUDDY, null, "")
        assertTrue(estimate.fromFallback)
        assertTrue(estimate.totalLow < estimate.totalKcal && estimate.totalKcal < estimate.totalHigh)
        assertTrue(estimate.personaReaction.isNotBlank())

        val workout = composite.buildWorkout(Persona.COACH, "strength", 30, "none")
        assertTrue(workout.fromFallback)
        assertEquals(30, workout.totalMinutes)

        val swaps = composite.suggestSwaps(Persona.BUDDY, listOf("fries"))
        assertTrue(swaps.fromFallback)
        assertEquals(1, swaps.swaps.size)
    }

    @Test
    fun `no backend configured means pure offline mode`() = runTest {
        val composite = CompositeAgentService(null, FallbackAgentService())
        assertTrue(composite.estimateMeal(Persona.BUDDY, null, "").fromFallback)
    }
}
