package com.yukigasai.trinkaus.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.SendMessageResult
import com.yukigasai.trinkaus.shared.WearableMessenger
import com.yukigasai.trinkaus.shared.getDefaultAmount
import com.yukigasai.trinkaus.shared.getDisplayName
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import com.yukigasai.trinkaus.shared.isMetric
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.NotificationWorker
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPopup(
    stateHolder: TrinkAusStateHolder,
    updateShowSettingsModal: (Boolean) -> Unit,
) {
    val hydrationGoal = stateHolder.hydrationGoal.collectAsState(initial = 2.0)
    val reminderEnabled = stateHolder.isReminderEnabled.collectAsState(false)
    val reminderDespiteGoal = stateHolder.reminderDespiteGoal.collectAsState(false)
    val reminderStartTime = stateHolder.startTime.collectAsState(0f)
    val reminderEndTime = stateHolder.endTime.collectAsState(24f)
    val isHideKonfettiEnabled = stateHolder.isHideKonfettiEnabled.collectAsState(false)
    val useGraphHistory = stateHolder.useGraphHistory.collectAsState(false)
    var showNoNodesDialog by remember { mutableStateOf(false) }
    var isWearApiAvailable by remember { mutableStateOf(true) }

    val smallAmount =
        stateHolder.smallAmount.collectAsState(
            HydrationOption.SMALL.getDefaultAmount(),
        )
    val mediumAmount =
        stateHolder.mediumAmount.collectAsState(
            HydrationOption.MEDIUM.getDefaultAmount(),
        )
    val largeAmount =
        stateHolder.largeAmount.collectAsState(
            HydrationOption.LARGE.getDefaultAmount(),
        )

    val context = LocalContext.current
    val dataStore = DataStoreSingleton.getInstance(context)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(key1 = Unit) {
        isWearApiAvailable = WearableMessenger.isWearableApiAvailable(context)
    }

    val WEAR_APP_PACKAGE_NAME = "com.yukigasai.trinkaus"
    val WEAR_OS_PACKAGE_NAME = "com.google.android.wearable.app"

    fun openPlayStoreForWearApp(
        context: Context,
        packageId: String,
    ) {
        try {
            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageId"),
                )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$packageId"),
                    )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(context, "Could not open Play Store.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { updateShowSettingsModal(false) },
        sheetState = sheetState,
        modifier = Modifier.imePadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = spacedBy(16.dp),
        ) {
            OptionSection {
                Text(
                    text = stringResource(R.string.water_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                HorizontalDivider(
                    thickness = 2.dp,
                    modifier = Modifier.width(200.dp).padding(bottom = 4.dp),
                )

                Text(
                    text = stringResource(R.string.daily_water_intake_goal),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = getVolumeStringWithUnit(hydrationGoal.value),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Slider(
                    value = hydrationGoal.value.toFloat(),
                    onValueChange = {
                        scope.launch(Dispatchers.IO) {
                            dataStore.edit { preferences ->
                                preferences[DataStoreKeys.HYDRATION_GOAL] =
                                    if (isMetric()) {
                                        (it * 10).roundToInt() / 10.0
                                    } else {
                                        it
                                            .toInt()
                                            .toDouble()
                                    }
                            }
                        }
                    },
                    onValueChangeFinished = {
                        scope.launch(Dispatchers.IO) {
                            WearableMessenger.sendMessage(
                                context,
                                Constants.Path.UPDATE_GOAL,
                                hydrationGoal.value,
                            )
                        }
                    },
                    valueRange = if (isMetric()) 1f..10f else 1f..200.0f,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = stringResource(R.string.intake_amounts_setting),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = spacedBy(16.dp),
                ) {
                    WaterIntakeItem(
                        hydrationOption = HydrationOption.SMALL,
                        modifier = Modifier.weight(1f),
                        initialAmount = smallAmount.value,
                        sheetState = sheetState,
                        onAmountChange = {
                            scope.launch(Dispatchers.IO) {
                                dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.SMALL_AMOUNT] =
                                        it.toIntOrNull() ?: HydrationOption.SMALL.getDefaultAmount()
                                }
                            }
                        },
                    )
                    WaterIntakeItem(
                        hydrationOption = HydrationOption.MEDIUM,
                        modifier = Modifier.weight(1f),
                        initialAmount = mediumAmount.value,
                        sheetState = sheetState,
                        onAmountChange = {
                            scope.launch(Dispatchers.IO) {
                                dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.MEDIUM_AMOUNT] =
                                        it.toIntOrNull()
                                            ?: HydrationOption.MEDIUM.getDefaultAmount()
                                }
                            }
                        },
                    )
                    WaterIntakeItem(
                        hydrationOption = HydrationOption.LARGE,
                        modifier = Modifier.weight(1f),
                        initialAmount = largeAmount.value,
                        sheetState = sheetState,
                        onAmountChange = {
                            scope.launch(Dispatchers.IO) {
                                dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.LARGE_AMOUNT] =
                                        it.toIntOrNull() ?: HydrationOption.LARGE.getDefaultAmount()
                                }
                            }
                        },
                    )
                }
            }

            OptionSection {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.enable_reminders),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = reminderEnabled.value,
                        onCheckedChange = { isChecked ->
                            scope.launch(Dispatchers.IO) {
                                dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.IS_REMINDER_ENABLED] = isChecked
                                }
                            }
                        },
                    )
                }

                HorizontalDivider(
                    thickness = 2.dp,
                    modifier = Modifier.width(200.dp).padding(bottom = 4.dp),
                )

                Text(
                    text = stringResource(R.string.reminder_time_range),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = "${reminderStartTime.value.toInt()}:00 - ${reminderEndTime.value.toInt()}:00",
                    style = MaterialTheme.typography.displaySmall,
                    color =
                        if (reminderEnabled.value) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        },
                )

                RangeSlider(
                    enabled = reminderEnabled.value,
                    value = reminderStartTime.value..reminderEndTime.value,
                    onValueChange = { values ->
                        scope.launch(Dispatchers.IO) {
                            dataStore.edit { preferences ->
                                preferences[DataStoreKeys.REMINDER_START_TIME] =
                                    values.start
                                preferences[DataStoreKeys.REMINDER_END_TIME] =
                                    values.endInclusive
                            }
                        }
                    },
                    valueRange = 0f..24f,
                    steps = 23,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.reminder_despite_goal),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        enabled = reminderEnabled.value,
                        checked = reminderDespiteGoal.value && reminderEnabled.value,
                        onCheckedChange = { isChecked ->
                            scope.launch(Dispatchers.IO) {
                                dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.REMINDER_DESPITE_GOAL] =
                                        isChecked
                                }
                            }
                        },
                    )
                }

                TextButton(
                    enabled = reminderEnabled.value,
                    onClick = {
                        val workRequest =
                            OneTimeWorkRequestBuilder<NotificationWorker>()
                                .setInputData(workDataOf("isTestNotification" to true))
                                .build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                    colors =
                        ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.test_notification),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            OptionSection {
                Text(
                    text = stringResource(R.string.general),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                HorizontalDivider(
                    thickness = 2.dp,
                    modifier = Modifier.width(200.dp).padding(bottom = 4.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.hide_konfetti),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = isHideKonfettiEnabled.value,
                        onCheckedChange = { isChecked ->
                            scope.launch(Dispatchers.IO) {
                                dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.HIDE_KONFETTI] = isChecked
                                }
                            }
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.use_graph_history),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Switch(
                        checked = useGraphHistory.value,
                        onCheckedChange = { isChecked ->
                            scope.launch(Dispatchers.IO) {
                                dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.USE_GRAPH_HISTORY] = isChecked
                                }
                            }
                        },
                    )
                }

                TextButton(
                    onClick = {
                        HydrationHelper.openSettings(context)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                    colors =
                        ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        text = "Health Connect ${stringResource(R.string.settings)}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                TextButton(
                    onClick = {
                        scope.launch {
                            val result =
                                WearableMessenger.sendMessage(
                                    context = context,
                                    path = Constants.Path.TEST_NOTIFICATION,
                                    msg = "This is a test message from your phone!",
                                )

                            // Show feedback to the user based on the result
                            when (result) {
                                is SendMessageResult.Success -> {
                                    Toast.makeText(context, "Test message sent successfully!", Toast.LENGTH_SHORT).show()
                                }
                                is SendMessageResult.NoNodesFound -> {
                                    showNoNodesDialog = true
                                }
                                is SendMessageResult.ApiNotAvailable -> {
                                    isWearApiAvailable = false
                                }
                                is SendMessageResult.Error -> {
                                    Toast.makeText(context, "Failed to send message. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                    // Add padding here
                    colors =
                        ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Text(
                        text = "Test Watch Connection",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                if (!isWearApiAvailable || showNoNodesDialog) {
                    Card(
                        modifier = Modifier.padding(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val text =
                                if (!isWearApiAvailable) {
                                    "The Wear API is not available on your device. Please ensure your watch is connected and the companion app is installed"
                                } else {
                                    "The TrinkAus WearOS App is not installed on your watch. Please install it to sync data."
                                }
                            val packageId =
                                if (!isWearApiAvailable) {
                                    WEAR_OS_PACKAGE_NAME
                                } else {
                                    WEAR_APP_PACKAGE_NAME
                                }

                            Text(
                                text = text,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(
                                modifier = Modifier.height(8.dp),
                            )
                            TextButton(
                                onClick = {
                                    openPlayStoreForWearApp(context, packageId)
                                },
                                colors =
                                    ButtonDefaults.textButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                            ) {
                                Text(
                                    text = "Open Play Store",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }

                RepositoryButton()
            }
        }
    }
}

@Composable
fun OptionSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterIntakeItem(
    hydrationOption: HydrationOption,
    initialAmount: Int,
    sheetState: SheetState,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Card(
        modifier = modifier.width(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(hydrationOption.icon),
                contentDescription = hydrationOption.getDisplayName(context),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            TextField(
                modifier =
                    Modifier.onFocusChanged {
                        if (it.hasFocus) {
                            scope.launch(Dispatchers.Main) {
                                // Stupid way to prevent keyboard from hiding bottom sheet
                                delay(700)
                                sheetState.expand()
                            }
                        }
                    },
                value = initialAmount.toString(),
                onValueChange = { newValue ->
                    onAmountChange(newValue.trim())
                },
                textStyle =
                    TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                    ),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = Done,
                    ),
                singleLine = true,
                maxLines = 1,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = hydrationOption.getDisplayName(context),
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
