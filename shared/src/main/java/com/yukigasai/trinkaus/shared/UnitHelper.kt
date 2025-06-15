package com.yukigasai.trinkaus.shared

import android.content.Context
import android.icu.util.LocaleData
import android.icu.util.ULocale
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object UnitHelper {
    // init based on the default locale's measurement system.
    private var isMetric: Boolean = LocaleData.getMeasurementSystem(ULocale.getDefault()) == LocaleData.MeasurementSystem.SI

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val dataStore = DataStoreSingleton.getInstance(context)
            isMetric =
                dataStore.data
                    .map { prefs ->
                        prefs[Constants.DataStore.DataStoreKeys.IS_METRIC] != false
                    }.first()
        }
    }

    fun setMetric(
        context: Context,
        value: Boolean,
        updateWatch: Boolean = false,
    ) {
        isMetric = value
        CoroutineScope(Dispatchers.IO).launch {
            val dataStore = DataStoreSingleton.getInstance(context)
            dataStore.edit { preferences ->
                preferences[Constants.DataStore.DataStoreKeys.IS_METRIC] = value
            }
            if (updateWatch) {
                WearableMessenger.sendMessage(
                    context = context,
                    path = Constants.Path.UPDATE_UNIT,
                    msg = value.toString(),
                )
            }
        }
    }

    fun isMetric(): Boolean = isMetric

    fun getUnit(): String = if (isMetric()) "L" else "fl oz"

    fun getVolumeString(volume: Double): String =
        String
            .format("%.3f", volume)
            .trimEnd('0')
            .trimEnd('.')
            .trimEnd(',')

    fun getVolumeStringWithUnit(volume: Double): String = "${getVolumeString(volume)} ${getUnit()}"
}
