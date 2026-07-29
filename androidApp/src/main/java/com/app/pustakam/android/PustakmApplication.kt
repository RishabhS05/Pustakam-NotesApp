package com.app.pustakam.android

import android.app.Application
import com.app.pustakam.android.di.getAndroidSpecifics
import com.app.pustakam.koin.initKoin
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

import org.koin.core.logger.Level

class PustakmApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        initKoin {
            FirebaseApp.initializeApp(this@PustakmApplication)
            androidLogger(level = Level.INFO)
            androidContext(this@PustakmApplication)
            modules(getAndroidSpecifics())
        }
    }
}