package com.yukigasai.trinkaus.shared

object Constants {
    object Preferences {
        const val PREFS_NAME = "TRINKAUS_HYDRATION_PREFS"
        const val HYDRATION_GOAL_KEY = "hydration_goal"
        const val HYDRATION_KEY = "hydration_level"
    }

    object Notification {
        const val CHANNEL_ID = "hydration_reminder_channel"
        const val CHANNEL_NAME = "Hydration Reminder"
        const val CHANNEL_DESCRIPTION = "Channel for hydration reminders"
        const val MESSAGE_TITLE = "Time to Drink Water"
        const val MESSAGE_CONTENT = "Remember to drink water to stay hydrated!"
    }


    object Path {
        const val REQUEST_HYDRATION = "/request_hydration"
        const val UPDATE_HYDRATION = "/update_hydration"
        const val ADD_HYDRATION = "/add_hydration"
        const val UPDATE_GOAL = "/update_goal"
    }

    object IntentAction {
        const val NEW_HYDRATION = "NEW_HYDRATION"
    }

    object IntentKey {
        const val HYDRATION_DATA = "hydration_data"
        const val HYDRATION_GOAL = "hydration_goal"
    }

}