package br.edu.utfpr.cc.setac.wotsiapp

import android.app.Application
import br.edu.utfpr.cc.setac.wotsiapp.di.appModule
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class ChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ChatApplication)
            modules(appModule)
        }
    }
}

