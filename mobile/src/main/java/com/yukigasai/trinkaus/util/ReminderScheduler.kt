package com.yukigasai.trinkaus.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys.REMINDER_INTERVAL
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val REMINDER_WORK_TAG = "hydration_reminder_work"

    suspend fun startOrRescheduleReminders(context: Context) {
        val dataStore = DataStoreSingleton.getInstance(context)
        val reminderIntervalMinutes =
            dataStore.data
                .map { prefs ->
                    prefs[REMINDER_INTERVAL] ?: 60
                }.first()

        stopReminders(context)

        // Create the first OneTimeWorkRequest
        val initialRequest =
            OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(reminderIntervalMinutes.toLong(), TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            REMINDER_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            initialRequest,
        )
    }

    fun stopReminders(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_TAG)
    }
}
