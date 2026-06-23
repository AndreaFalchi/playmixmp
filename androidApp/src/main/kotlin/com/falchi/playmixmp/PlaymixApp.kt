package com.falchi.playmixmp

import android.app.Application

class PlaymixApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Logger
        Logger.initialize(AndroidFileLogger(this))
        
        // Log basic info
        Logger.i("Application started")
        
        // Setup Uncaught Exception Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Logger.e("Uncaught exception in thread ${thread.name}", throwable)
            
            // Allow system to handle the crash (shows the "App has stopped" dialog)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
