package com.yukigasai.trinkaus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.WearableMessenger
import com.yukigasai.trinkaus.shared.getAmount
import com.yukigasai.trinkaus.util.HydrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
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

            // Hide the message
            with(NotificationManagerCompat.from(context)) {
                cancel(Constants.Notification.MESSAGE_ID)
            }
        }
    }
}
