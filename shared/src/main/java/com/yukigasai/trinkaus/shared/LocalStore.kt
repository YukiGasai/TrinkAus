package com.yukigasai.trinkaus.shared

import android.content.Context

object LocalStore {
    fun save(context: Context, key: String, value: Double) {
        val prefs = context.getSharedPreferences(Constants.Preferences.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(key, value.toFloat()).apply()
    }

    fun load(context: Context, key: String): Double {
        val prefs = context.getSharedPreferences(Constants.Preferences.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(key, 0.0F).toDouble()
    }
}