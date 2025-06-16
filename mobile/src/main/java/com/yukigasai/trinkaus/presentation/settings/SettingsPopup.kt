package com.yukigasai.trinkaus.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.presentation.RepositoryButton
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.UnitHelper
import com.yukigasai.trinkaus.util.ReminderScheduler
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPopup(
    stateHolder: TrinkAusStateHolder,
    updateShowSettingsModal: (Boolean) -> Unit,
) {
    val isMetric = stateHolder.isMetric.collectAsState(initial = true)
    val options = listOf("L/mL", "fl. oz.")
    var isMetricIndex by remember { mutableIntStateOf(if (UnitHelper.isMetric()) 0 else 1) }
    val reminderEnabled = stateHolder.isReminderEnabled.collectAsState(false)
    val reminderDespiteGoal = stateHolder.reminderDespiteGoal.collectAsState(false)
    val reminderStartTime = stateHolder.startTime.collectAsState(8f)
    val reminderEndTime = stateHolder.endTime.collectAsState(23f)
    val reminderCustomSound = stateHolder.reminderCustomSound.collectAsState(false)
    val isHideKonfettiEnabled = stateHolder.isHideKonfettiEnabled.collectAsState(false)
    val useGraphHistory = stateHolder.useGraphHistory.collectAsState(false)
    var unitSystemChanged by remember { mutableStateOf(false) }

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
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    ModalBottomSheet(
        onDismissRequest = { updateShowSettingsModal(false) },
        sheetState = sheetState,
        modifier = Modifier.imePadding(),
    ) {
        key(isMetric.value) {
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
                                        UnitHelper.setMetric(
                                            context,
                                            isMetricButton,
                                            updateWatch = true,
                                        )
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

                    HydrationGoalSelector()

                    SettingsSubTitle(stringResource(R.string.intake_amounts_setting))

                    WaterAmountSetting(sheetState)
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

                    ReminderIntervalSelector(reminderEnabled.value)

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

                    TestNotificationButton(
                        reminderEnabled.value,
                    )
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

                    HealthConnectButton()

                    TestWearConnectionButton()

                    RepositoryButton()
                }
            }
        }
    }
}
