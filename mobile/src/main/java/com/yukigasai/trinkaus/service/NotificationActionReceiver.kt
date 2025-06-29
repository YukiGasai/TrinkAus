package com.yukigasai.trinkaus.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.updateAll
import com.yukigasai.trinkaus.service.CompanionMessageService
import com.yukigasai.trinkaus.service.WaterServerService.Companion.triggerNotificationUpdate
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.WearableMessenger
import com.yukigasai.trinkaus.shared.getAmount
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.ServerManager
import com.yukigasai.trinkaus.widget.TrinkAusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    @SuppressLint("ServiceCast")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action
        when (action) {
            Constants.IntentAction.ADD_SMALL -> {
                updateIntake(context, HydrationOption.SMALL)
            }

            Constants.IntentAction.ADD_MEDIUM -> {
                updateIntake(context, HydrationOption.MEDIUM)
            }

            Constants.IntentAction.ADD_LARGE -> {
                updateIntake(context, HydrationOption.LARGE)
            }

            Constants.IntentAction.ACTION_COPY_IP -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val serverUrlResult = ServerManager.getServerUrl()
                        if (serverUrlResult.isSuccess) {
                            val serverUrl = serverUrlResult.getOrThrow()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Server URL", serverUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Server URL copied!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Server not running or IP not available.", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun updateIntake(
        context: Context,
        option: HydrationOption,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val amount = option.getAmount(context)

            HydrationHelper.writeHydrationLevel(context, amount)
            val newHydration = HydrationHelper.readHydrationLevel(context)

            WearableMessenger.sendMessage(
                context,
                Constants.Path.UPDATE_HYDRATION,
                newHydration,
            )

            val dataStore = DataStoreSingleton.getInstance(context)
            dataStore.edit { preferences ->
                preferences[DataStoreKeys.HYDRATION_LEVEL] = newHydration
            }

            // Update all the widgets manually as the flow might be dead
            TrinkAusWidget().updateAll(context)

            triggerNotificationUpdate(context)

            // Hide the message
            with(NotificationManagerCompat.from(context)) {
                cancel(Constants.Notification.MESSAGE_ID)
            }
        }
    }
}
