package com.yukigasai.trinkaus.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.WorkManager
import com.yukigasai.trinkaus.service.AlarmReceiver
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys.REMINDER_INTERVAL
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val REMINDER_WORK_TAG = "hydration_reminder_work"
    private const val ALARM_REQUEST_CODE = 1112

    suspend fun startOrRescheduleReminders(context: Context) {
        val dataStore = DataStoreSingleton.getInstance(context)
        val reminderIntervalMinutes =
            dataStore.data
                .map { prefs ->
                    prefs[REMINDER_INTERVAL] ?: 60
                }.first()

        stopReminders(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val triggerAtMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(reminderIntervalMinutes.toLong())
        Log.d("Trinkaus", "Scheduling next alarm for $reminderIntervalMinutes minutes. Trigger time: ${java.util.Date(triggerAtMillis)}")

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }

    fun stopReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        alarmManager.cancel(pendingIntent)

        Log.d("Trinkaus", "Stopping reminders and cancelling scheduled alarms.")
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_TAG)
    }
}
