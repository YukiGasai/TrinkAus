package com.yukigasai.trinkaus.service

import android.content.Intent
import androidx.wear.tiles.TileService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.LocalStore

class WatchMessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        println("Received message: ${messageEvent.path}")
        when (messageEvent.path) {

             Constants.Path.UPDATE_GOAL -> {
                val goal = messageEvent.data.toString(Charsets.UTF_8).toDouble()
                val intent = Intent(Constants.IntentAction.NEW_HYDRATION).apply {
                    putExtra(Constants.IntentKey.HYDRATION_GOAL, goal)
                }
                LocalStore.save(this, Constants.Preferences.HYDRATION_GOAL_KEY, goal)
                TileService.getUpdater(this@WatchMessageService).requestUpdate(HydrationTileService::class.java)
                sendBroadcast(intent)
            }

            Constants.Path.UPDATE_HYDRATION -> {
                val hydration = messageEvent.data.toString(Charsets.UTF_8).toDouble()
                val intent = Intent(Constants.IntentAction.NEW_HYDRATION).apply {
                    putExtra(Constants.IntentKey.HYDRATION_DATA, hydration)
                }
                LocalStore.save(this@WatchMessageService, Constants.Preferences.HYDRATION_KEY, hydration)
                TileService.getUpdater(this@WatchMessageService).requestUpdate(HydrationTileService::class.java)
                sendBroadcast(intent)
            }

            else -> super.onMessageReceived(messageEvent)
        }
    }
}