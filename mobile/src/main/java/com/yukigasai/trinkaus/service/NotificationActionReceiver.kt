package com.yukigasai.trinkaus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.presentation.dataStore
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.shared.isMetric
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.HydrationOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            Constants.IntentAction.ADD_SMALL -> {
                updateIntake(context, 0)
            }

            Constants.IntentAction.ADD_MEDIUM -> {
                updateIntake(context, 1)
            }

            Constants.IntentAction.ADD_LARGE -> {
                updateIntake(context, 2)
            }
        }
    }

    private fun updateIntake(context: Context, hydrationOptionIndex: Int) {
        val amount = if (isMetric()) {
            HydrationOption.all[hydrationOptionIndex].amountMetric
        } else {
            HydrationOption.all[hydrationOptionIndex].amountUS
        }

        CoroutineScope(Dispatchers.Main).launch {
            HydrationHelper.writeHydrationLevel(context, amount)
            val newHydration = HydrationHelper.readHydrationLevel(context)

            SendMessageThread(
                context,
                Constants.Path.UPDATE_HYDRATION,
                newHydration
            ).start()

            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.HYDRATION_LEVEL] = newHydration
            }

            // Hide the message
            with(NotificationManagerCompat.from(context)) {
                cancel(Constants.Notification.MESSAGE_ID)
            }
        }
    }
}
