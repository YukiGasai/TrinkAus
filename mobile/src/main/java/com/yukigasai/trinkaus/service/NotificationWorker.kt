package com.yukigasai.trinkaus.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.LocalStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class NotificationWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val goal = LocalStore.load(context, Constants.Preferences.HYDRATION_GOAL_KEY)
        if (currentHour in 8..21) {
            CoroutineScope(Dispatchers.IO).launch {
                val hydration = HydrationHelper.readHydrationLevel(context)
                if (hydration < goal) {
                    sendNotification(context)
                }
            }
        }
        return Result.success()
    }

    private fun sendNotification(context: Context) {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            Constants.Notification.CHANNEL_ID, Constants.Notification.CHANNEL_NAME, importance
        ).apply {
            description = Constants.Notification.CHANNEL_DESCRIPTION
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(context, Constants.Notification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(Constants.Notification.MESSAGE_TITLE)
            .setContentText(Constants.Notification.MESSAGE_CONTENT)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(1, builder.build())
        }
    }
}