package com.judgemycal.app

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.judgemycal.app.data.AgentApi
import com.judgemycal.app.data.AgentService
import com.judgemycal.app.data.AuthInterceptor
import com.judgemycal.app.data.BackendAgentService
import com.judgemycal.app.data.CompositeAgentService
import com.judgemycal.app.data.FallbackAgentService
import com.judgemycal.app.data.MealRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Minimal service locator. Production wiring happens in [JudgeMyCalApp];
 * instrumented tests replace [agentService] with a fake before launching
 * activities.
 */
object AppGraph {

    lateinit var mealRepository: MealRepository
    lateinit var agentService: AgentService

    val initialized: Boolean
        get() = ::agentService.isInitialized

    fun initProduction(context: Context) {
        mealRepository = MealRepository(context)

        val backendUrl = BuildConfig.BACKEND_URL.trim().let {
            if (it.isEmpty() || it.endsWith("/")) it else "$it/"
        }
        val backend = if (backendUrl.isEmpty()) null else {
            val client = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                // Multi-agent LLM calls are slow; give the whole call a generous cap.
                .callTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build()
            val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
            val api = Retrofit.Builder()
                .baseUrl(backendUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AgentApi::class.java)
            BackendAgentService(
                api = api,
                sessionStore = mealRepository,
                uidProvider = ::ensureSignedIn,
            )
        }
        agentService = CompositeAgentService(backend, FallbackAgentService())
    }

    private suspend fun ensureSignedIn(): String {
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { return it.uid }
        // Anonymous sign-in is enough for v1 — no real accounts (spec §4).
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: throw IllegalStateException("Anonymous sign-in returned no user")
    }
}
