package com.clawforge.app

import android.app.Application

class ClawForgeApp : Application() {
    companion object {
        lateinit var ctx: ClawForgeApp
            private set
    }
    override fun onCreate() {
        super.onCreate()
        ctx = this
    }
}
