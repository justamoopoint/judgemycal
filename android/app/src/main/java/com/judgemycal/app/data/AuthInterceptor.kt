package com.judgemycal.app.data

import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the Firebase ID token (and, when available, an App Check token) to
 * every backend call — the one place auth headers are added, per the spec,
 * instead of at each call site. The binary carries no key of any kind; the
 * bearer token is a short-lived, per-user Firebase credential.
 */
class AuthInterceptor(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val appCheck: FirebaseAppCheck? = runCatching { FirebaseAppCheck.getInstance() }.getOrNull(),
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val user = auth.currentUser
            ?: throw IOException("Not signed in to Firebase yet")

        val idToken = try {
            Tasks.await(user.getIdToken(false), 15, TimeUnit.SECONDS).token
        } catch (e: Exception) {
            throw IOException("Could not obtain Firebase ID token", e)
        } ?: throw IOException("Firebase returned an empty ID token")

        val builder = chain.request().newBuilder()
            .header("Authorization", "Bearer $idToken")

        // Best-effort App Check: the backend only enforces it once you deploy
        // with REQUIRE_APP_CHECK=1 (RUNBOOK §7), so a missing token here must
        // not block the request during rollout.
        appCheck?.let {
            runCatching {
                Tasks.await(it.getAppCheckToken(false), 5, TimeUnit.SECONDS).token
            }.getOrNull()?.let { token ->
                builder.header("X-Firebase-AppCheck", token)
            }
        }

        return chain.proceed(builder.build())
    }
}
