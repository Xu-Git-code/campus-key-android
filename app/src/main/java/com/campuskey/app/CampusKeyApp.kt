package com.campuskey.app

import android.app.Application

class CampusKeyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}
