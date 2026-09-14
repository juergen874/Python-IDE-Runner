package com.example

import android.app.Application
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class PythonIdeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
                Log.d("PythonIdeApplication", "Chaquopy Python initialized successfully in Application.onCreate")
            }
        } catch (e: Throwable) {
            Log.e("PythonIdeApplication", "Failed to start Python in Application.onCreate: ${e.message}", e)
        }
    }
}
