package com.yukigasai.trinkaus.shared

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.firstOrNull

enum class HydrationOption(
    val icon: Int,
    val nameId: Int,
    val dataStoreKey: Preferences.Key<Int>,
) {
    SMALL(
        icon = R.drawable.glass_small_icon,
        nameId = R.string.small,
        dataStoreKey = Constants.DataStore.DataStoreKeys.SMALL_AMOUNT,
    ),
    MEDIUM(
        icon = R.drawable.glass_icon,
        nameId = R.string.medium,
        dataStoreKey = Constants.DataStore.DataStoreKeys.MEDIUM_AMOUNT,
    ),
    LARGE(
        icon = R.drawable.bottle_icon,
        nameId = R.string.large,
        dataStoreKey = Constants.DataStore.DataStoreKeys.LARGE_AMOUNT,
    ),
}

fun HydrationOption.getDefaultAmount(): Int {
    val isMetric = UnitHelper.isMetric()
    return when (this) {
        HydrationOption.SMALL -> if (isMetric) 125 else 4
        HydrationOption.MEDIUM -> if (isMetric) 250 else 8
        HydrationOption.LARGE -> if (isMetric) 500 else 16
    }
}

fun HydrationOption.getDisplayName(context: Context): String = context.getString(nameId)

suspend fun HydrationOption.getAmount(context: Context): Int {
    val dataStore = DataStoreSingleton.getInstance(context)
    val preferences = dataStore.data.firstOrNull() ?: return getDefaultAmount()

    return when (this) {
        HydrationOption.SMALL ->
            preferences[Constants.DataStore.DataStoreKeys.SMALL_AMOUNT]
                ?: getDefaultAmount()
        HydrationOption.MEDIUM ->
            preferences[Constants.DataStore.DataStoreKeys.MEDIUM_AMOUNT]
                ?: getDefaultAmount()
        HydrationOption.LARGE ->
            preferences[Constants.DataStore.DataStoreKeys.LARGE_AMOUNT]
                ?: getDefaultAmount()
    }
}
