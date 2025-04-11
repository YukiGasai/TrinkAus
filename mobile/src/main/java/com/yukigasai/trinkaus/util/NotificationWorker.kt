package com.yukigasai.trinkaus.util

import android.Manifest
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
import com.yukigasai.trinkaus.service.NotificationActionReceiver
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.LocalStore
import com.yukigasai.trinkaus.shared.getVolumeString
import com.yukigasai.trinkaus.shared.isMetric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour < 8 || currentHour >= 23) {
            return@withContext Result.success()
        }

        val hydrationGoal = LocalStore.load(context, Constants.Preferences.HYDRATION_GOAL_KEY)
        val currentIntake = HydrationHelper.readHydrationLevel(context)

        if (currentIntake >= hydrationGoal) {
            return@withContext Result.success()
        }
        showNotification(context)
        return@withContext Result.success()
    }

    private fun showNotification(context: Context) {
        val channel = NotificationChannel(
            Constants.Notification.CHANNEL_ID,
            Constants.Notification.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = Constants.Notification.CHANNEL_DESCRIPTION
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context,  Constants.Notification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(Constants.Notification.MESSAGE_TITLE)
            .setContentText(Constants.Notification.MESSAGE_CONTENT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            listOf(
                Constants.IntentAction.ADD_SMALL,
                Constants.IntentAction.ADD_MEDIUM,
                Constants.IntentAction.ADD_LARGE
            ).forEachIndexed { index, action ->
                val option = HydrationOption.all[index]

                val addIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    this.action = action
                }
                val addPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        index,
                        addIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                val value = if (isMetric()) option.amountMetric else option.amountUS

                builder.addAction(
                    option.icon,
                    "+${getVolumeString(value)}",
                    addPendingIntent
                )
            }

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(Constants.Notification.MESSAGE_ID, builder.build())
        }
    }
}
