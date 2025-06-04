package com.yukigasai.trinkaus.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.SendMessageThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate

class TrinkAusStateHolder(
    private val context: Context, private val dataStore: DataStore<Preferences>
) {

    val isLoading = mutableStateOf(false)
    val selectedDate = mutableStateOf(LocalDate.now())

    val hydrationLevel: Flow<Double> = dataStore.data.map { preferences ->
            preferences[DataStoreKeys.HYDRATION_LEVEL] ?: 0.0
        }

    val hydrationGoal: Flow<Double> = dataStore.data.map { preferences ->
            preferences[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
        }

    val isReminderEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DataStoreKeys.IS_REMINDER_ENABLED] == true
        }

    val reminderDespiteGoal: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[DataStoreKeys.REMINDER_DESPITE_GOAL] == true
    }

    val startTime: Flow<Float> = dataStore.data.map { preferences ->
            preferences[DataStoreKeys.REMINDER_START_TIME] ?: 0.0f
        }

    val endTime: Flow<Float> = dataStore.data.map { preferences ->
            preferences[DataStoreKeys.REMINDER_END_TIME] ?: 0.0f
        }

    private suspend fun _refreshDataFromSource() {
        val hydration = HydrationHelper.readHydrationLevel(context)
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.HYDRATION_LEVEL] = hydration
        }
    }

    fun refreshDataFromSource() {
        CoroutineScope(Dispatchers.IO).launch {
            isLoading.value = true
            _refreshDataFromSource()
            isLoading.value = false
        }
    }

    fun addHydration(newLevel: Double) {
        CoroutineScope(Dispatchers.Main).launch {
            isLoading.value = true
            HydrationHelper.writeHydrationLevel(context, newLevel)
            _refreshDataFromSource()

            val currentLevel = hydrationLevel.firstOrNull() ?: 0.0

            SendMessageThread(
                context, Constants.Path.UPDATE_HYDRATION, currentLevel
            ).start()
            isLoading.value = false
        }
    }
}