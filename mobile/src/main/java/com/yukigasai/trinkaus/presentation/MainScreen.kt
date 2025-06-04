package com.yukigasai.trinkaus.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.shared.getVolumeString
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelHelperProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.TimeUnit


private suspend fun checkPermissions(
    healthConnectClient: HealthConnectClient, healthPermissions: Set<String>
): Boolean {
    val permissions = healthConnectClient.permissionController.getGrantedPermissions()
    return permissions.containsAll(healthPermissions)
}

fun convertToGraphFormat(
    historyData: Map<LocalDate, Double>, color: Color
): List<Bars> {
    return historyData.map {
        Bars(
            label = it.key.dayOfMonth.toString(), values = listOf(
                Bars.Data(
                    value = it.value, color = SolidColor(color)
                )
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    stateHolder: TrinkAusStateHolder,
    healthConnectClient: HealthConnectClient,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hydrationLevel = stateHolder.hydrationLevel.collectAsState(0.0)
    val hydrationGoal = stateHolder.hydrationGoal.collectAsState(0.1)

    val showConfetti = remember { mutableStateOf(false) }

    val permissionGranted = remember { mutableStateOf<Boolean?>(null) }
    val showSettingsModal = remember { mutableStateOf(false) }
    val isLoading = remember { stateHolder.isLoading }
    val selectedDate = remember { stateHolder.selectedDate }
    val maxValue = remember { mutableStateOf(0.0) }
    val isLoadingHistory = remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val graphData = remember { mutableStateListOf<Bars>() }
    var historyJob: Job? = null
    val healthPermissions = setOf(
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        scope.launch {
            if (checkPermissions(healthConnectClient, healthPermissions)) {
                permissionGranted.value = true
                stateHolder.refreshDataFromSource()
            } else {
                permissionGranted.value = false
            }
        }
    }
    var confettiJob: Job? = null
    LaunchedEffect(showConfetti.value) {
        confettiJob?.cancel()
        confettiJob = scope.launch(Dispatchers.IO) {
            if (showConfetti.value) {
                delay(2000)
                showConfetti.value = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (checkPermissions(healthConnectClient, healthPermissions)) {
            permissionGranted.value = true
            stateHolder.refreshDataFromSource()
        } else {
            permissionGranted.value = false
            permissionLauncher.launch(healthPermissions)
        }
    }

    LaunchedEffect(selectedDate.value) {
        historyJob?.cancel() // Vorherige Coroutine abbrechen, falls sie noch läuft
        historyJob = scope.launch(Dispatchers.IO) {
            isLoadingHistory.value = true
            try {
                val newHydrationData =
                    HydrationHelper.getHydrationHistoryForMonth(context, selectedDate.value)
                maxValue.value = (newHydrationData.values.maxOrNull() ?: 0.0) + 1
                graphData.clear()
                // Fetch hydration history for the selected month
                graphData.addAll(
                    convertToGraphFormat(
                        newHydrationData, primaryColor
                    )
                )

            } finally {
                isLoadingHistory.value = false
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            actions = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable { showSettingsModal.value = true },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
        )
    }) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxSize()
            ) {
                when (permissionGranted.value) {
                    null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = stringResource(R.string.checking_permissions),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    false -> {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.permissions_required),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = stringResource(R.string.permissions_request_text),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilledTonalButton(
                                    onClick = { permissionLauncher.launch(healthPermissions) },
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Text(
                                        text = stringResource(R.string.grant_permissions),
                                    )
                                }
                            }
                        }
                    }

                    true -> {
                        val scrollState = rememberScrollState()

                        PullToRefreshBox(
                            isRefreshing = isLoading.value,
                            onRefresh = {
                                stateHolder.refreshDataFromSource()
                            },
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)

                            ) {
                                Spacer(
                                    modifier = Modifier.height(16.dp)
                                )
                                Card(
                                    modifier = Modifier.size(300.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                    shape = CircleShape
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        CircularProgressIndicator(
                                            progress = {
                                                (hydrationLevel.value / hydrationGoal.value).coerceIn(
                                                    0.0, 1.0
                                                ).toFloat()
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            strokeWidth = 16.dp,
                                            strokeCap = StrokeCap.Round
                                        )

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = getVolumeString(hydrationLevel.value),
                                                style = MaterialTheme.typography.displayMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Text(
                                                text = "/${getVolumeStringWithUnit(hydrationGoal.value)}",
                                                style = MaterialTheme.typography.headlineSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }

                                if (hydrationGoal.value > 0) {
                                    Text(
                                        text = "${((hydrationLevel.value / hydrationGoal.value) * 100).toInt()}% ${stringResource(R.string.of_goal)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }

                                AddHydrationButtons {
                                    showConfetti.value = true
                                    stateHolder.addHydration(it)
                                    SendMessageThread(
                                        context, Constants.Path.UPDATE_HYDRATION, it
                                    ).start()
                                }

                                HorizontalDivider()

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 2.dp
                                ) {
                                    val showDatePickerDialog = remember { mutableStateOf(false) }
                                    val datePickerState = rememberDatePickerState(
                                        initialSelectedDateMillis = selectedDate.value.toEpochDay() * 24 * 60 * 60 * 1000L,
                                        yearRange = 2010..selectedDate.value.year,
                                    )

                                    if (showDatePickerDialog.value) {
                                        DatePickerDialog(
                                            onDismissRequest = { showDatePickerDialog.value = false },
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    showDatePickerDialog.value = false
                                                    datePickerState.selectedDateMillis?.let { millis ->
                                                        selectedDate.value = Instant.ofEpochMilli(millis)
                                                            .atZone(ZoneId.systemDefault())
                                                            .toLocalDate()
                                                    }
                                                }) {
                                                    Text("OK")
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDatePickerDialog.value = false }) {
                                                    Text(stringResource(R.string.cancel))
                                                }
                                            }
                                        ) {
                                            DatePicker(
                                                showModeToggle = true,
                                               state = datePickerState,
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        IconButton(onClick = {
                                            selectedDate.value = selectedDate.value.minusMonths(1)
                                        }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                                contentDescription = "Previous Month",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        val monthName = selectedDate.value.month.getDisplayName(
                                            java.time.format.TextStyle.FULL, Locale.getDefault()
                                        )
                                        Text(
                                            text = "$monthName ${selectedDate.value.year}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).clickable {
                                                showDatePickerDialog.value = true
                                            }
                                        )

                                        IconButton(
                                            onClick = {
                                                selectedDate.value = selectedDate.value.plusMonths(1)
                                            },
                                            enabled = LocalDate.now() > selectedDate.value
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = "Next Month",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                if (isLoadingHistory.value) {
                                    CircularProgressIndicator()
                                } else {
                                    if (graphData.isNotEmpty()) {
                                        ColumnChart(
                                            modifier = Modifier.fillMaxWidth().height(300.dp).padding(bottom = 10.dp),
                                            maxValue = maxValue.value,
                                            labelProperties = LabelProperties(
                                                enabled = true,
                                                rotation = LabelProperties.Rotation(
                                                    degree = -90f,
                                                    padding = 10.dp
                                                ),
                                                textStyle = TextStyle(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                                    fontWeight = FontWeight.Normal
                                                ),
                                            ),
                                            popupProperties = PopupProperties(
                                                textStyle = TextStyle(
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                containerColor = Color(MaterialTheme.colorScheme.secondaryContainer.value),
                                                contentBuilder = {  dataIndex, valueIndex, value ->
                                                    "${graphData[dataIndex].label}: ${getVolumeStringWithUnit(value)}"
                                                }
                                            ),
                                            indicatorProperties = HorizontalIndicatorProperties(
                                                textStyle = TextStyle(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                                    fontWeight = FontWeight.Normal
                                                ),
                                            ),
                                            labelHelperProperties = LabelHelperProperties(enabled = false),
                                            gridProperties = GridProperties(enabled = false),
                                            barProperties = BarProperties(
                                                cornerRadius = Bars.Data.Radius.Circular(2.dp),
                                                thickness = 8.dp
                                            ),
                                            data = graphData,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.no_history_data),
                                            modifier = Modifier.padding(vertical = 32.dp),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (showConfetti.value) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(), parties = listOf(
                        Party(
                            speed = 10f,
                            maxSpeed = 30f,
                            damping = 0.9f,
                            spread = 180,
                            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                            emitter = Emitter(
                                duration = 100, TimeUnit.MILLISECONDS
                            ).perSecond(2000),
                            position = Position.Relative(0.5, 1.0),
                            angle = -90
                        )
                    )
                )
            }
        }

        if (showSettingsModal.value) {
            SettingsPopup(
                stateHolder = stateHolder,
                updateShowSettingsModal = { showSettingsModal.value = it },
            )
        }
    }
}