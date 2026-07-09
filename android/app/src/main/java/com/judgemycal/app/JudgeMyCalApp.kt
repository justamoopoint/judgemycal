package com.judgemycal.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck

class JudgeMyCalApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Guarded init: with the placeholder google-services.json (or none at
        // all) the app still runs — in offline demo mode. Real Firebase wiring
        // is a runbook step, not a build-time requirement.
        runCatching {
            FirebaseApp.initializeApp(this)
            // Debug builds install the App Check debug provider; release builds
            // install Play Integrity (per-build-type AppCheckInstaller source sets).
            AppCheckInstaller.install(FirebaseAppCheck.getInstance())
        }.onFailure { Log.w("JudgeMyCal", "Firebase unavailable — offline demo mode", it) }

        if (!AppGraph.initialized) {
            AppGraph.initProduction(this)
        }
    }
}
