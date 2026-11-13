package com.bisariya.oxycarb

import android.app.Application
import android.util.Log
import org.schabi.newpipe.extractor.NewPipe

class OxycarbApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            NewPipe.init(AppDownloader(this))
        } catch (e: Exception) {
            Log.e("OxycarbApp", "Failed to initialize NewPipe", e)
        }
    }
}
