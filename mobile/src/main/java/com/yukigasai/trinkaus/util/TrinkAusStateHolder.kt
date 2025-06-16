package com.yukigasai.trinkaus.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.WearableMessenger
import com.yukigasai.trinkaus.shared.getAmount
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
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

class TrinkAusStateHolder(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    val scope = CoroutineScope(Dispatchers.IO)
    val isLoading = mutableStateOf(false)
    val selectedDate = mutableStateOf(LocalDate.now())

    val isMetric: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.IS_METRIC] == true
        }

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
            preferences[DataStoreKeys.REMINDER_START_TIME] ?: 8.0f
        }

    val endTime: Flow<Float> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.REMINDER_END_TIME] ?: 23.0f
        }

    val reminderCustomSound: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.REMINDER_CUSTOM_SOUND] == true
        }

    val isHideKonfettiEnabled: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.HIDE_KONFETTI] == true
        }

    val useGraphHistory: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[DataStoreKeys.USE_GRAPH_HISTORY] == true
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

            WearableMessenger.sendMessage(
                context,
                Constants.Path.UPDATE_HYDRATION,
                currentLevel,
            )
            isLoading.value = false
        }
    }

    fun shareWaterIntake(
        context: Context,
        graphicsLayer: GraphicsLayer,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val screenshotBitmap = graphicsLayer.toImageBitmap()
            val imageUri = saveBitmapAndGetUri(context, screenshotBitmap)
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    // Grant permission for the receiving app to read the URI
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_title)))
        }
    }
}

private fun saveBitmapAndGetUri(
    context: Context,
    bitmap: ImageBitmap,
): Uri? {
    return try {
        val cacheDir = context.cacheDir ?: return null
        // Create a directory for the images
        val imagePath = File(cacheDir, "images")
        imagePath.mkdirs()

        // Create the image file
        val file = File(imagePath, "shared_image.png")

        // Save the bitmap to the file
        val outputStream = FileOutputStream(file)
        bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        outputStream.flush()
        outputStream.close()

        // Get the content URI using the FileProvider
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
