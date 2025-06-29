package com.yukigasai.trinkaus.shared

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constants {
    object DataStore {
        const val FILE_NAME = "trinkaus_hydration_prefs"

        object DataStoreKeys {
            val IS_METRIC = booleanPreferencesKey("is_metric")
            val HYDRATION_LEVEL = doublePreferencesKey("hydration_level")
            val HYDRATION_GOAL = doublePreferencesKey("hydration_goal")
            val IS_REMINDER_ENABLED = booleanPreferencesKey("is_reminder_enabled")
            val REMINDER_DESPITE_GOAL = booleanPreferencesKey("reminder_despite_goal")
            val REMINDER_START_TIME = floatPreferencesKey("reminder_start_time")
            val REMINDER_END_TIME = floatPreferencesKey("reminder_end_time")
            val REMINDER_INTERVAL = intPreferencesKey("reminder_interval")
            val REMINDER_CUSTOM_SOUND = booleanPreferencesKey("reminder_custom_sound")
            val HIDE_KONFETTI = booleanPreferencesKey("hide_konfetti")
            val USE_GRAPH_HISTORY = booleanPreferencesKey("use_graph_history")
            val SMALL_AMOUNT = intPreferencesKey("small_amount")
            val MEDIUM_AMOUNT = intPreferencesKey("medium_amount")
            val LARGE_AMOUNT = intPreferencesKey("large_amount")
            val USE_LOCAL_SERVER = booleanPreferencesKey("use_local_server")
            val AUTH_TOKEN = stringPreferencesKey("auth_token")
        }
    }

    object Notification {
        const val CHANNEL_ID = "hydration_reminder_channel"
        const val CHANNEL_ID_CUSTOM_SOUND = "hydration_reminder_channel_custom_sound"
        const val CHANNEL_NAME = "Hydration Reminder"
        const val CHANNEL_DESCRIPTION = "Channel for hydration reminders"
        const val MESSAGE_ID = 1
    }

    object Path {
        const val REQUEST_HYDRATION = "/request_hydration"
        const val UPDATE_HYDRATION = "/update_hydration"
        const val ADD_HYDRATION = "/add_hydration"
        const val UPDATE_GOAL = "/update_goal"
        const val TEST_NOTIFICATION = "/test_notification"
        const val UPDATE_UNIT = "/update_unit"
    }

    object IntentAction {
        const val ADD_SMALL = "ADD_SMALL"
        const val ADD_MEDIUM = "ADD_MEDIUM"
        const val ADD_LARGE = "ADD_LARGE"
        const val ACTION_COPY_IP = "ACTION_COPY_IP"
    }
}
