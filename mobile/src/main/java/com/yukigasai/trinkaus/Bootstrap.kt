package com.yukigasai.trinkaus

import android.app.Application
import com.yukigasai.trinkaus.shared.UnitHelper

class Bootstrap : Application() {
    override fun onCreate() {
        super.onCreate()
        UnitHelper.initialize(this)
    }
}
