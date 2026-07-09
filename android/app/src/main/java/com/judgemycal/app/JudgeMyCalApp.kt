package com.judgemycal.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class JudgeMyCalApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Guarded init: with the placeholder google-services.json (or none at
        // all) the app still runs — in offline demo mode. Real Firebase wiring
        // is a runbook step, not a build-time requirement.
        runCatching {
            FirebaseApp.initializeApp(this)
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance(),
                )
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance(),
                )
            }
        }.onFailure { Log.w("JudgeMyCal", "Firebase unavailable — offline demo mode", it) }

        if (!AppGraph.initialized) {
            AppGraph.initProduction(this)
        }
    }
}
