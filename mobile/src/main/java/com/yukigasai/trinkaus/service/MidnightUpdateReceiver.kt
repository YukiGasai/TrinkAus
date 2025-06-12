package com.yukigasai.trinkaus.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.glance.appwidget.updateAll
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys.HYDRATION_LEVEL
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.util.scheduleMidnightUpdate
import com.yukigasai.trinkaus.widget.TrinkAusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MidnightUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataStore = DataStoreSingleton.getInstance(context)
                dataStore.edit {
                    it[HYDRATION_LEVEL] = 0.0
                }

                TrinkAusWidget().updateAll(context)

                scheduleMidnightUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
