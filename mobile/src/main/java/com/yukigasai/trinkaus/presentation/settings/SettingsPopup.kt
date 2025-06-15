package com.yukigasai.trinkaus.presentation.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.presentation.RepositoryButton
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.SendMessageResult
import com.yukigasai.trinkaus.shared.UnitHelper
import com.yukigasai.trinkaus.shared.WearableMessenger
import com.yukigasai.trinkaus.shared.getDefaultAmount
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.NotificationWorker
import com.yukigasai.trinkaus.util.ReminderScheduler
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPopup(
    stateHolder: TrinkAusStateHolder,
    updateShowSettingsModal: (Boolean) -> Unit,
) {
    val options = listOf("L/mL", "fl. oz.")
    var isMetricIndex by remember { mutableIntStateOf(if (UnitHelper.isMetric()) 0 else 1) }
    val hydrationGoal = stateHolder.hydrationGoal.collectAsState(initial = 2.0)
    val reminderEnabled = stateHolder.isReminderEnabled.collectAsState(false)
    val reminderDespiteGoal = stateHolder.reminderDespiteGoal.collectAsState(false)
    val reminderStartTime = stateHolder.startTime.collectAsState(8f)
    val reminderEndTime = stateHolder.endTime.collectAsState(23f)
    val reminderInterval = stateHolder.reminderInterval.collectAsState(60)
    val reminderCustomSound = stateHolder.reminderCustomSound.collectAsState(false)
    val isHideKonfettiEnabled = stateHolder.isHideKonfettiEnabled.collectAsState(false)
    val useGraphHistory = stateHolder.useGraphHistory.collectAsState(false)
    var showNoNodesDialog by remember { mutableStateOf(false) }
    var isWearApiAvailable by remember { mutableStateOf(true) }
    var unitSystemChanged by remember { mutableStateOf(false) }

    val smallAmount =
        stateHolder.smallAmount.collectAsState(null)
    val mediumAmount =
        stateHolder.mediumAmount.collectAsState(null)
    val largeAmount =
        stateHolder.largeAmount.collectAsState(null)

    val context = LocalContext.current
    val dataStore = DataStoreSingleton.getInstance(context)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                scope.launch(Dispatchers.IO) {
                    dataStore.edit { preferences ->
                        preferences[DataStoreKeys.IS_REMINDER_ENABLED] = true
                    }
                    ReminderScheduler.startOrRescheduleReminders(context)
                }
            } else {
                Toast
                    .makeText(
                        context,
                        context.getString(R.string.reminder_permission_denied),
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }

    fun isNotificationPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(key1 = Unit) {
        isWearApiAvailable = WearableMessenger.isWearableApiAvailable(context)
    }

    fun openPlayStoreForWearApp(
        context: Context,
        packageId: String,
    ) {
        try {
            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    "market://details?id=$packageId".toUri(),
                )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://play.google.com/store/apps/details?id=$packageId".toUri(),
                    )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(context, context.getString(R.string.open_play_store_error), Toast.LENGTH_SHORT).show()
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
            OptionSection(
                headerTitle = stringResource(R.string.water_settings),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    SettingsSubTitle(stringResource(R.string.unit_system))

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        options.forEachIndexed { index, label ->
                            SegmentedButton(
                                modifier = Modifier.weight(1f),
                                shape =
                                    SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size,
                                    ),
                                onClick = {
                                    isMetricIndex = index
                                    val isMetricButton = index == 0
                                    unitSystemChanged = true
                                    UnitHelper.setMetric(context, isMetricButton, updateWatch = true)
                                },
                                selected = index == isMetricIndex,
                                label = { Text(label) },
                            )
                        }
                    }
                }

                if (unitSystemChanged) {
                    Text(
                        text = stringResource(R.string.units_changed, options[isMetricIndex]),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier.padding(bottom = 8.dp).clickable {
                                unitSystemChanged = false
                            },
                    )
                }

                SettingsSubTitle(stringResource(R.string.daily_water_intake_goal))

                Text(
                    text = UnitHelper.getVolumeStringWithUnit(hydrationGoal.value),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                Slider(
                    value = hydrationGoal.value.toFloat(),
                    onValueChange = {
                        scope.launch(Dispatchers.IO) {
                            val amount =
                                if (UnitHelper.isMetric()) {
                                    (it * 10).roundToInt() / 10.0
                                } else {
                                    it
                                        .toInt()
                                        .toDouble()
                                }

                            dataStore.edit { preferences ->
                                preferences[DataStoreKeys.HYDRATION_GOAL] = amount
                            }
                            WearableMessenger.sendMessage(
                                context = context,
                                path = Constants.Path.UPDATE_GOAL,
                                msg = amount,
                            )
                        }
                    },
                    valueRange = if (UnitHelper.isMetric()) 1f..10f else 1f..200.0f,
                    modifier = Modifier.fillMaxWidth(),
                )

                SettingsSubTitle(stringResource(R.string.intake_amounts_setting))

                val amounts =
                    listOf(
                        HydrationOption.SMALL to smallAmount,
                        HydrationOption.MEDIUM to mediumAmount,
                        HydrationOption.LARGE to largeAmount,
                    )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = spacedBy(16.dp),
                ) {
                    amounts.forEach { (hydrationOption, amountState) ->
                        if (amountState.value == null) {
                            CircularProgressIndicator()
                        } else {
                            WaterIntakeItem(
                                hydrationOption = hydrationOption,
                                modifier = Modifier.weight(1f),
                                initialAmount = amountState.value!!,
                                sheetState = sheetState,
                                onAmountChange = {
                                    scope.launch(Dispatchers.IO) {
                                        dataStore.edit { preferences ->
                                            preferences[hydrationOption.dataStoreKey] =
                                                it.toIntOrNull() ?: hydrationOption.getDefaultAmount()
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            OptionSection(
                headerTitle = stringResource(R.string.enable_reminders),
                headerContent = {
                    Switch(
                        checked = reminderEnabled.value,
                        onCheckedChange = { isChecked ->
                            if (isChecked &&
                                !isNotificationPermissionGranted() &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ) {
                                permissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                                return@Switch
                            }

                            scope.launch(Dispatchers.IO) {
                                dataStore.edit { preferences ->
                                    preferences[DataStoreKeys.IS_REMINDER_ENABLED] = isChecked
                                }
                                if (isChecked) {
                                    ReminderScheduler.startOrRescheduleReminders(context)
                                } else {
                                    ReminderScheduler.stopReminders(context)
                                }
                            }
                        },
                    )
                },
            ) {
                SettingsSubTitle(stringResource(R.string.reminder_time_range))

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

                SettingsSubTitle(stringResource(R.string.reminder_interval))

                Text(
                    text = "${reminderInterval.value} ${stringResource(R.string.minutes)}",
                    style = MaterialTheme.typography.displaySmall,
                    color =
                        if (reminderEnabled.value) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        },
                )

                Slider(
                    enabled = reminderEnabled.value,
                    value = reminderInterval.value.toFloat(),
                    onValueChangeFinished = {
                        scope.launch(Dispatchers.IO) {
                            ReminderScheduler.startOrRescheduleReminders(context)
                        }
                    },
                    onValueChange = { values ->
                        scope.launch(Dispatchers.IO) {
                            dataStore.edit { preferences ->
                                preferences[DataStoreKeys.REMINDER_INTERVAL] = values.toInt()
                            }
                        }
                    },
                    valueRange = 15f..720f,
                    steps = 705,
                    modifier = Modifier.fillMaxWidth(),
                )

                SwitchWithLabel(
                    isEnabled = reminderEnabled.value,
                    labelText = stringResource(R.string.reminder_despite_goal),
                    isChecked = reminderDespiteGoal.value && reminderEnabled.value,
                    onCheckedChange = { isChecked ->
                        scope.launch(Dispatchers.IO) {
                            dataStore.edit { preferences ->
                                preferences[DataStoreKeys.REMINDER_DESPITE_GOAL] = isChecked
                            }
                        }
                    },
                )

                SwitchWithLabel(
                    isEnabled = reminderEnabled.value,
                    labelText = stringResource(R.string.custom_reminder_sound),
                    isChecked = reminderCustomSound.value && reminderEnabled.value,
                    onCheckedChange = { isChecked ->
                        scope.launch(Dispatchers.IO) {
                            dataStore.edit { preferences ->
                                preferences[DataStoreKeys.REMINDER_CUSTOM_SOUND] = isChecked
                            }
                        }
                    },
                )

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

            OptionSection(
                headerTitle = stringResource(R.string.general),
            ) {
                SwitchWithLabel(
                    labelText = stringResource(R.string.hide_konfetti),
                    isChecked = isHideKonfettiEnabled.value,
                    onCheckedChange = { isChecked ->
                        scope.launch(Dispatchers.IO) {
                            dataStore.edit { preferences ->
                                preferences[DataStoreKeys.HIDE_KONFETTI] = isChecked
                            }
                        }
                    },
                )

                SwitchWithLabel(
                    labelText = stringResource(R.string.use_graph_history),
                    isChecked = useGraphHistory.value,
                    onCheckedChange = { isChecked ->
                        scope.launch(Dispatchers.IO) {
                            dataStore.edit { preferences ->
                                preferences[DataStoreKeys.USE_GRAPH_HISTORY] = isChecked
                            }
                        }
                    },
                )

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
                                    msg = context.getString(R.string.wear_os_test_message),
                                )

                            // Show feedback to the user based on the result
                            when (result) {
                                is SendMessageResult.Success -> {
                                    Toast.makeText(context, context.getText(R.string.wear_os_test_send_success), Toast.LENGTH_SHORT).show()
                                }
                                is SendMessageResult.NoNodesFound -> {
                                    showNoNodesDialog = true
                                }
                                is SendMessageResult.ApiNotAvailable -> {
                                    isWearApiAvailable = false
                                }
                                is SendMessageResult.Error -> {
                                    Toast.makeText(context, context.getString(R.string.wear_os_test_send_error), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
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
                        text = stringResource(R.string.test_watch_connection),
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
                                    stringResource(R.string.wear_api_error)
                                } else {
                                    stringResource(R.string.wear_companion_error)
                                }
                            val packageId =
                                if (!isWearApiAvailable) {
                                    "com.google.android.wearable.app"
                                } else {
                                    context.packageName
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
