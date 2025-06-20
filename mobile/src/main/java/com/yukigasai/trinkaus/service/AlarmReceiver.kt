package com.yukigasai.trinkaus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.NotificationHelper
import com.yukigasai.trinkaus.util.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action == "START_FOREGROUND_SERVICE") {
            val serviceIntent =
                Intent(context, WaterServerService::class.java).apply {
                    action = WaterServerService.ACTION_START
                }
            context.startForegroundService(serviceIntent)
            return
        }

        Log.d("Trinkaus", "AlarmReceiver: Triggered.")

        val pendingResult = goAsync()

        scope.launch {
            try {
                ReminderScheduler.startOrRescheduleReminders(context)

                val dataStore = DataStoreSingleton.getInstance(context)

                val isReminderEnabled = dataStore.data.first()[Constants.DataStore.DataStoreKeys.IS_REMINDER_ENABLED] ?: false
                Log.d("Trinkaus", "AlarmReceiver: Is reminder enabled? $isReminderEnabled")
                if (!isReminderEnabled) {
                    return@launch
                }

                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val startTime = dataStore.data.first()[Constants.DataStore.DataStoreKeys.REMINDER_START_TIME] ?: 8f
                val endTime = dataStore.data.first()[Constants.DataStore.DataStoreKeys.REMINDER_END_TIME] ?: 23f

                Log.d("Trinkaus", "AlarmReceiver: Checking time window. CurrentHour=$currentHour, StartTime=$startTime, EndTime=$endTime")
                if (currentHour < startTime || currentHour >= endTime) {
                    return@launch
                }

                val hydrationGoal = dataStore.data.first()[Constants.DataStore.DataStoreKeys.HYDRATION_GOAL] ?: 2.0
                val reminderDespiteGoal = dataStore.data.first()[Constants.DataStore.DataStoreKeys.REMINDER_DESPITE_GOAL] ?: false
                val currentIntake = HydrationHelper.readHydrationLevel(context)

                Log.d(
                    "Trinkaus",
                    "AlarmReceiver: Checking goal. CurrentIntake=$currentIntake, Goal=$hydrationGoal, ReminderDespiteGoal=$reminderDespiteGoal",
                )
                if (currentIntake >= hydrationGoal && !reminderDespiteGoal) {
                    return@launch
                }

                val percentage = ((currentIntake / hydrationGoal) * 100).toInt()

                Log.d("Trinkaus", "AlarmReceiver: All checks passed. Preparing to show notification.")
                NotificationHelper.showNotification(context, currentIntake, percentage)
            } finally {
                // Always finish the async processing to avoid memory leaks
                pendingResult.finish()
                Log.d("Trinkaus", "AlarmReceiver: Async work finished.")
            }
        }
    }
}
