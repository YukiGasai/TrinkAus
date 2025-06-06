package com.yukigasai.trinkaus.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.util.StreakResult
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

private suspend fun checkPermissions(
    healthConnectClient: HealthConnectClient,
    healthPermissions: Set<String>,
): Boolean {
    val permissions = healthConnectClient.permissionController.getGrantedPermissions()
    return permissions.containsAll(healthPermissions)
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
    val largestStreak = stateHolder.largestStreak.collectAsState(StreakResult())
    val currentStreak = stateHolder.currentStreak.collectAsState(StreakResult())
    val useGraphHistory = stateHolder.useGraphHistory.collectAsState(false)

    val showConfetti = remember { mutableStateOf(false) }

    val permissionGranted = remember { mutableStateOf<Boolean?>(null) }
    val showSettingsModal = remember { mutableStateOf(false) }
    val isLoading = remember { stateHolder.isLoading }

    val isHideKonfettiEnabled = stateHolder.isHideKonfettiEnabled.collectAsState(false)

    val healthPermissions =
        setOf(
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class),
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
        )

    val permissionLauncher =
        rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
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
        confettiJob =
            scope.launch(Dispatchers.IO) {
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

    val spacing = 22.dp

    Scaffold { padding ->

        when (permissionGranted.value) {
            null -> PermissionLoadingScreen(padding)
            false ->
                RequestPermissionScreen(padding) {
                    permissionLauncher.launch(healthPermissions)
                }
            true -> {
                val scrollState = rememberScrollState()

                PullToRefreshBox(
                    isRefreshing = isLoading.value,
                    onRefresh = {
                        stateHolder.refreshDataFromSource()
                    },
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing),
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                    ) {
                        Spacer(
                            modifier = Modifier.height(spacing),
                        )

                        CurrentHydrationDisplay(
                            hydrationLevel = hydrationLevel.value,
                            hydrationGoal = hydrationGoal.value,
                        )

                        // Make sure we don't divide by zero
                        if (hydrationGoal.value > 0) {
                            Text(
                                text = "${((hydrationLevel.value / hydrationGoal.value) * 100).toInt()}% ${stringResource(
                                    R.string.of_goal,
                                )}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        AddHydrationButtons {
                            showConfetti.value = true
                            stateHolder.addHydration(it)
                            SendMessageThread(
                                context,
                                Constants.Path.UPDATE_HYDRATION,
                                it,
                            ).start()
                        }

                        HorizontalDivider()

                        StreakDisplay(
                            largestStreak = largestStreak.value,
                            currentStreak = currentStreak.value,
                        )

                        HistoryMonthSelector(stateHolder)

                        if (useGraphHistory.value) {
                            HistoricHydrationDisplay(stateHolder)
                        } else {
                            HydrationCalendar(stateHolder)
                        }
                    }

                    IconButton(
                        modifier = Modifier.align(Alignment.TopEnd),
                        onClick = {
                            showSettingsModal.value = true
                        },
                    ) {
                        Icon(
                            imageVector = Lucide.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(spacing),
                )
            }
        }

        if (showConfetti.value && !isHideKonfettiEnabled.value) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties =
                    listOf(
                        Party(
                            speed = 10f,
                            maxSpeed = 30f,
                            damping = 0.9f,
                            spread = 180,
                            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                            emitter =
                                Emitter(
                                    duration = 100,
                                    TimeUnit.MILLISECONDS,
                                ).perSecond(2000),
                            position = Position.Relative(0.5, 1.0),
                            angle = -90,
                        ),
                    ),
            )
        }

        if (showSettingsModal.value) {
            SettingsPopup(
                stateHolder = stateHolder,
                updateShowSettingsModal = { showSettingsModal.value = it },
            )
        }
    }
}

@Composable
fun PermissionLoadingScreen(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.checking_permissions),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RequestPermissionScreen(
    padding: PaddingValues,
    getPermission: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.permissions_required),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.permissions_request_text),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = getPermission,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = stringResource(R.string.grant_permissions),
                )
            }
        }
    }
}
