package com.yukigasai.trinkaus.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.yukigasai.trinkaus.service.AlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationStarter : BroadcastReceiver() {
    private val tag = "NotificationStarter"

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(tag, "BOOT_COMPLETED received.")

            // Tell the system we are doing async work
            val pendingResult: PendingResult = goAsync()

            // You can use a predefined scope or launch a new one
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(tag, "Starting async work in BroadcastReceiver.")
                    scheduleMidnightUpdate(context)
                    ReminderScheduler.startOrRescheduleReminders(context)

                    // If the Local Server is enabled, start it via WorkManager
                    if (ServerManager.isEnabled(context)) {
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                        // Create the same intent that we want to receive in the service
                        val serviceIntent =
                            Intent(context, AlarmReceiver::class.java).apply {
                                action = "START_FOREGROUND_SERVICE"
                            }

                        // Create a PendingIntent that will start the service
                        val pendingIntent =
                            PendingIntent.getBroadcast(
                                context,
                                1187,
                                serviceIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                            )

                        val triggerAtMillis = System.currentTimeMillis() + 90000

                        try {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    triggerAtMillis,
                                    pendingIntent,
                                )
                                Log.d(tag, "Exact alarm scheduled to start service in 15 seconds.")
                            } else {
                                // Fallback for when exact alarms are not permitted
                                alarmManager.setAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    triggerAtMillis,
                                    pendingIntent,
                                )
                                Log.d(tag, "Standard alarm scheduled to start service in 15 seconds.")
                            }
                        } catch (se: SecurityException) {
                            Log.e(tag, "Could not schedule alarm. Is SCHEDULE_EXACT_ALARM permission granted?", se)
                        }
                    }
                } finally {
                    // CRITICAL: Always call finish() when you're done.
                    Log.d(tag, "Async work finished, releasing receiver.")
                    pendingResult.finish()
                }
            }
        }
    }
}
