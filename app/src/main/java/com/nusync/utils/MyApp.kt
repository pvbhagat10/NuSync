package com.nusync.utils

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.BuildConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        if (BuildConfig.DEBUG) {
            Log.d("BUILD_CONFIG_CHECK", "BuildConfig.DEBUG is TRUE")
        } else {
            Log.d("BUILD_CONFIG_CHECK", "BuildConfig.DEBUG is FALSE")
        }

        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            Log.i("firebaseAppCheck", "DebugAppCheckProviderFactory")
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            Log.i("firebaseAppCheck", "PlayIntegrityAppCheckProviderFactory")
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}