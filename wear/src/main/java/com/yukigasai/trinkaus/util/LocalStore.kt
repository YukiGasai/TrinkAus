package com.yukigasai.trinkaus.util

import android.content.Context

object LocalStore {

    const val PREFS_NAME = "TRINKAUS_HYDRATION_PREFS"
    const val HYDRATION_KEY = "hydration"
    const val HYDRATION_GOAL_KEY = "hydration_goal"

    fun save(context: Context, key: String, value: Double) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(key, value.toFloat()).apply()
    }

    fun load(context: Context, key: String): Double {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(key, 0.0F).toDouble()
    }
}