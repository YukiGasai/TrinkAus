package com.yukigasai.trinkaus.service

import androidx.datastore.preferences.core.edit
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.yukigasai.trinkaus.presentation.dataStore
import com.yukigasai.trinkaus.shared.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WatchMessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        println("Received message: ${messageEvent.path}")
        when (messageEvent.path) {
            Constants.Path.UPDATE_GOAL -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val goal = messageEvent.data.toString(Charsets.UTF_8).toDouble()

                    this@WatchMessageService.dataStore.edit { preferences ->
                        preferences[Constants.DataStore.DataStoreKeys.HYDRATION_GOAL] = goal
                    }
                    TileService
                        .getUpdater(this@WatchMessageService)
                        .requestUpdate(HydrationTileService::class.java)
                }
            }

            Constants.Path.UPDATE_HYDRATION -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val hydration = messageEvent.data.toString(Charsets.UTF_8).toDouble()

                    this@WatchMessageService.dataStore.edit { preferences ->
                        preferences[Constants.DataStore.DataStoreKeys.HYDRATION_LEVEL] = hydration
                    }
                    TileService
                        .getUpdater(this@WatchMessageService)
                        .requestUpdate(HydrationTileService::class.java)
                }
            }
            else -> super.onMessageReceived(messageEvent)
        }
    }
}
