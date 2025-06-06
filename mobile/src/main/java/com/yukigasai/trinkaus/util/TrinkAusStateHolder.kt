package com.yukigasai.trinkaus.util

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.shared.getAmount
import com.yukigasai.trinkaus.shared.getDefaultAmount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TrinkAusStateHolder(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    val isLoading = mutableStateOf(false)
    val selectedDate = mutableStateOf(LocalDate.now())

    val hydrationLevel: Flow<Double> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.HYDRATION_LEVEL] ?: 0.0
        }

    val hydrationGoal: Flow<Double> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
        }

    val isReminderEnabled: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.IS_REMINDER_ENABLED] == true
        }

    val reminderDespiteGoal: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.REMINDER_DESPITE_GOAL] == true
        }

    val startTime: Flow<Float> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.REMINDER_START_TIME] ?: 0.0f
        }

    val endTime: Flow<Float> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.REMINDER_END_TIME] ?: 0.0f
        }

    val isHideKonfettiEnabled: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.HIDE_KONFETTI] == true
        }

    val useGraphHistory: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.USE_GRAPH_HISTORY] == true
        }

    val smallAmount: Flow<Int> =
        dataStore.data.map { preferences ->
            val value = preferences[DataStoreKeys.SMALL_AMOUNT] ?: HydrationOption.SMALL.getDefaultAmount()
            println("Small Amount Loaded: $value") // Debugging
            value
        }

    val mediumAmount: Flow<Int> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.MEDIUM_AMOUNT] ?: HydrationOption.MEDIUM.getDefaultAmount()
        }

    val largeAmount: Flow<Int> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.LARGE_AMOUNT] ?: HydrationOption.LARGE.getDefaultAmount()
        }

    @OptIn(FlowPreview::class)
    val largestStreak: StateFlow<StreakResult> =
        combine(
            hydrationGoal,
            hydrationLevel,
        ) { goal, _ ->
            // Level is not used here, but we need to combine it to trigger updates
            HydrationHelper.getLongestWaterIntakeStreak(context, goal)
        }.debounce(500)
            .stateIn(
                scope = CoroutineScope(Dispatchers.IO),
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = StreakResult(),
            )

    @OptIn(FlowPreview::class)
    val currentStreak: StateFlow<StreakResult> =
        combine(
            hydrationGoal,
            hydrationLevel,
        ) { goal, _ ->
            // Level is not used here, but we need to combine it to trigger updates
            HydrationHelper.getCurrentWaterIntakeStreakLength(context, goal)
        }.debounce(500)
            .stateIn(
                scope = CoroutineScope(Dispatchers.IO),
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = StreakResult(),
            )

    private suspend fun getHydrationData() {
        val hydration = HydrationHelper.readHydrationLevel(context)
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.HYDRATION_LEVEL] = hydration
        }
    }

    fun refreshDataFromSource() {
        CoroutineScope(Dispatchers.IO).launch {
            isLoading.value = true
            getHydrationData()
            isLoading.value = false
        }
    }

    fun addHydration(hydrationOption: HydrationOption) {
        isLoading.value = true
        CoroutineScope(Dispatchers.Main).launch {
            val amountToAdd = hydrationOption.getAmount(context)
            HydrationHelper.writeHydrationLevel(context, amountToAdd)
            getHydrationData()

            val currentLevel = hydrationLevel.firstOrNull() ?: 0.0

            SendMessageThread(
                context,
                Constants.Path.UPDATE_HYDRATION,
                currentLevel,
            ).start()
            isLoading.value = false
        }
    }
}
