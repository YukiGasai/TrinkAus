package com.yukigasai.trinkaus.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification.AUDIO_ATTRIBUTES_DEFAULT
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.presentation.MainActivity
import com.yukigasai.trinkaus.service.NotificationActionReceiver
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.UnitHelper
import com.yukigasai.trinkaus.shared.getDisplayName
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
            Log.d("Trinkaus", "NotificationWorker: doWork started.")
            val context = applicationContext
            val dataStore = DataStoreSingleton.getInstance(context)
            if (!isTestNotification) {
                val isReminderEnabled =
                    dataStore.data.first()[DataStoreKeys.IS_REMINDER_ENABLED] == true
                Log.d("Trinkaus", "NotificationWorker: Is reminder enabled? $isReminderEnabled")
                if (!isReminderEnabled) {
                    return@withContext Result.success()
                }

                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val startTime = dataStore.data.first()[DataStoreKeys.REMINDER_START_TIME] ?: 8f
                val endTime = dataStore.data.first()[DataStoreKeys.REMINDER_END_TIME] ?: 23f

                Log.d(
                    "Trinkaus",
                    "NotificationWorker: Checking time window. CurrentHour=$currentHour, StartTime=$startTime, EndTime=$endTime",
                )
                if (currentHour < startTime || currentHour >= endTime) {
                    return@withContext Result.success()
                }
            }

            val hydrationGoal = dataStore.data.first()[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
            val reminderDespiteGoal =
                dataStore.data.first()[DataStoreKeys.REMINDER_DESPITE_GOAL] == true
            val currentIntake = HydrationHelper.readHydrationLevel(context)

            Log.d(
                "Trinkaus",
                "NotificationWorker: Checking goal. CurrentIntake=$currentIntake, Goal=$hydrationGoal, ReminderDespiteGoal=$reminderDespiteGoal",
            )
            if (currentIntake >= hydrationGoal && !reminderDespiteGoal && !isTestNotification) {
                return@withContext Result.success()
            }

            val percentage = ((currentIntake / hydrationGoal) * 100).toInt()

            Log.d("Trinkaus", "NotificationWorker: All checks passed. Preparing to show notification and reschedule.")

            ReminderScheduler.startOrRescheduleReminders(context)

            showNotification(context, currentIntake, percentage)
            return@withContext Result.success()
        }

    @SuppressLint("MissingPermission")
    private suspend fun showNotification(
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

        val dataStore = DataStoreSingleton.getInstance(context)
        val isCustomSoundEnabled =
            dataStore.data.first()[DataStoreKeys.REMINDER_CUSTOM_SOUND] == true

        val channel =
            if (isCustomSoundEnabled) {
                Constants.Notification.CHANNEL_ID_CUSTOM_SOUND
            } else {
                Constants.Notification.CHANNEL_ID
            }

        val builder =
            NotificationCompat
                .Builder(context, channel)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(
                    context.getString(
                        R.string.hydration_notification_title,
                    ),
                ).setContentText(
                    context.getString(
                        R.string.hydration_notification_text,
                        UnitHelper.getVolumeStringWithUnit(hydrationLevel),
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
        Log.d("Trinkaus", "NotificationWorker: Calling notificationManager.notify()")
        val notificationManager = NotificationManagerCompat.from(context)
        createOrUpdateNotificationChannel(context)
        notificationManager.notify(Constants.Notification.MESSAGE_ID, builder.build())
    }
}

fun createOrUpdateNotificationChannel(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val defaultChannel =
        NotificationChannel(
            Constants.Notification.CHANNEL_ID,
            Constants.Notification.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = Constants.Notification.CHANNEL_DESCRIPTION
        }

    notificationManager.createNotificationChannel(defaultChannel)

    val soundChannel =
        NotificationChannel(
            Constants.Notification.CHANNEL_ID_CUSTOM_SOUND,
            Constants.Notification.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = Constants.Notification.CHANNEL_DESCRIPTION
        }

    val soundUri = "android.resource://${context.packageName}/raw/notification_sound".toUri()

    soundChannel.setSound(soundUri, AUDIO_ATTRIBUTES_DEFAULT)
    notificationManager.createNotificationChannel(soundChannel)
}
