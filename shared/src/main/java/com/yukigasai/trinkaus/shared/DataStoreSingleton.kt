package com.yukigasai.trinkaus.shared

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

object DataStoreSingleton {
    private val Context.dataStoreInstance: DataStore<Preferences> by preferencesDataStore(name = Constants.DataStore.FILE_NAME)

    @Volatile
    private var INSTANCE: DataStore<Preferences>? = null

    fun getInstance(context: Context): DataStore<Preferences> =
        INSTANCE ?: synchronized(this) {
            val instance = context.dataStoreInstance
            INSTANCE = instance
            instance
        }
}
