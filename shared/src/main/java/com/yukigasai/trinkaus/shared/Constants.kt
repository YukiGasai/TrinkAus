package com.yukigasai.trinkaus.shared

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey

object Constants {

    object DataStore {
        const val FILE_NAME = "trinkaus_hydration_prefs"

        object DataStoreKeys {
            val HYDRATION_LEVEL = doublePreferencesKey("hydration_level")
            val HYDRATION_GOAL = doublePreferencesKey("hydration_goal")
            val IS_REMINDER_ENABLED = booleanPreferencesKey("is_reminder_enabled")
            val REMINDER_DESPITE_GOAL = booleanPreferencesKey("reminder_despite_goal")
            val REMINDER_START_TIME = floatPreferencesKey("reminder_start_time")
            val REMINDER_END_TIME = floatPreferencesKey("reminder_end_time")
        }
    }

    object Notification {
        const val CHANNEL_ID = "hydration_reminder_channel"
        const val CHANNEL_NAME = "Hydration Reminder"
        const val CHANNEL_DESCRIPTION = "Channel for hydration reminders"
        const val MESSAGE_ID = 1
        const val WORKER_TAG = "hydration_reminder_worker"
    }

    object Path {
        const val REQUEST_HYDRATION = "/request_hydration"
        const val UPDATE_HYDRATION = "/update_hydration"
        const val ADD_HYDRATION = "/add_hydration"
        const val UPDATE_GOAL = "/update_goal"
    }

    object IntentAction {
        const val ADD_SMALL = "ADD_SMALL"
        const val ADD_MEDIUM = "ADD_MEDIUM"
        const val ADD_LARGE = "ADD_LARGE"
    }
}