package com.yukigasai.trinkaus.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.presentation.MainActivity
import com.yukigasai.trinkaus.service.NotificationActionReceiver
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.getDisplayName
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val isTestNotification = inputData.getBoolean("isTestNotification", false)

            val context = applicationContext
            val dataStore = DataStoreSingleton.getInstance(context)
            if (!isTestNotification) {
                val isReminderEnabled =
                    dataStore.data.first()[DataStoreKeys.IS_REMINDER_ENABLED] == true

                if (!isReminderEnabled) {
                    return@withContext Result.success()
                }

                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val startTime = dataStore.data.first()[DataStoreKeys.REMINDER_START_TIME] ?: 8f
                val endTime = dataStore.data.first()[DataStoreKeys.REMINDER_END_TIME] ?: 23f

                if (currentHour < startTime || currentHour >= endTime) {
                    return@withContext Result.success()
                }
            }

            val hydrationGoal = dataStore.data.first()[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
            val reminderDespiteGoal =
                dataStore.data.first()[DataStoreKeys.REMINDER_DESPITE_GOAL] == true
            val currentIntake = HydrationHelper.readHydrationLevel(context)

            if (currentIntake >= hydrationGoal && !reminderDespiteGoal && !isTestNotification) {
                return@withContext Result.success()
            }

            val percentage = ((currentIntake / hydrationGoal) * 100).toInt()

            ReminderScheduler.startOrRescheduleReminders(context)

            showNotification(context, currentIntake, percentage)
            return@withContext Result.success()
        }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        context: Context,
        hydrationLevel: Double = 0.0,
        percentage: Int = 0,
    ) {
        // Make sure the app has permission to post notifications
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channel =
            NotificationChannel(
                Constants.Notification.CHANNEL_ID,
                Constants.Notification.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = Constants.Notification.CHANNEL_DESCRIPTION
            }

        val startAppIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        val startAppPendingIntent: PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                startAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat
                .Builder(context, Constants.Notification.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(
                    context.getString(
                        R.string.hydration_notification_title,
                    ),
                ).setContentText(
                    context.getString(
                        R.string.hydration_notification_text,
                        getVolumeStringWithUnit(hydrationLevel),
                        percentage,
                    ),
                ).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(startAppPendingIntent)
                .setAutoCancel(true)

        listOf(
            Constants.IntentAction.ADD_SMALL,
            Constants.IntentAction.ADD_MEDIUM,
            Constants.IntentAction.ADD_LARGE,
        ).forEachIndexed { index, action ->
            val option = HydrationOption.entries[index]

            val addIntent =
                Intent(context, NotificationActionReceiver::class.java).apply {
                    this.action = action
                }
            val addPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    index,
                    addIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            builder.addAction(
                option.icon,
                option.getDisplayName(context),
                addPendingIntent,
            )
        }

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(Constants.Notification.MESSAGE_ID, builder.build())
    }
}
