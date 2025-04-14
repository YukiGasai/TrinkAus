package com.yukigasai.trinkaus.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.SendMessageThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TrinkAusStateHolder(
    private val context: Context, private val dataStore: DataStore<Preferences>
) {
    private var updateGoalJob: Job? = null


    val hydrationLevel: Flow<Double> = dataStore.data.map { preferences ->
            preferences[DataStoreKeys.HYDRATION_LEVEL] ?: 0.0
        }

    val hydrationGoal: Flow<Double> = dataStore.data.map { preferences ->
            preferences[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
        }

    fun refreshDataFromSource() {
        SendMessageThread(
            context, Constants.Path.REQUEST_HYDRATION
        ).start()
    }

    fun addHydration(newLevel: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.edit { preferences ->
                preferences[DataStoreKeys.HYDRATION_LEVEL] = newLevel
            }
            SendMessageThread(
                context, Constants.Path.ADD_HYDRATION, newLevel
            ).start()
        }
    }

    fun updateGoal(newGoal: Double) {

        updateGoalJob?.cancel()
        updateGoalJob = CoroutineScope(Dispatchers.Main).launch {
            dataStore.edit { preferences ->
                preferences[DataStoreKeys.HYDRATION_GOAL] = newGoal
            }
            delay(1000)
            SendMessageThread(
                context, Constants.Path.UPDATE_GOAL, newGoal
            ).start()
        }
    }
}