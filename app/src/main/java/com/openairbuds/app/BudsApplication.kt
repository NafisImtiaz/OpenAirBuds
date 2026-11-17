package com.openairbuds.app

import android.app.Application
import com.openairbuds.app.model.ModelCatalog

class BudsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        ModelCatalog.init(this)
    }

    companion object {
        lateinit var instance: BudsApplication
            private set
    }
}
