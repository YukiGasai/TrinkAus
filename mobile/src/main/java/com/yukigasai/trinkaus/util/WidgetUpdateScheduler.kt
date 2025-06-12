package com.yukigasai.trinkaus.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.yukigasai.trinkaus.service.MidnightUpdateReceiver
import java.util.Calendar

fun scheduleMidnightUpdate(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val intent = Intent(context, MidnightUpdateReceiver::class.java)
    val pendingIntent =
        PendingIntent.getBroadcast(
            context,
            1111,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    // Calculate the time for the next midnight
    val calendar =
        Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1)
        }

    alarmManager.setAndAllowWhileIdle(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        pendingIntent,
    )
}

fun cancelMidnightUpdate(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, MidnightUpdateReceiver::class.java)
    val pendingIntent =
        PendingIntent.getBroadcast(
            context,
            1111,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
    if (pendingIntent != null) {
        alarmManager.cancel(pendingIntent)
    }
}
