package com.yukigasai.trinkaus.presentation

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.Lifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.util.OnLifecycleEvent
import com.yukigasai.trinkaus.util.StreakResult
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

sealed class HealthConnectState {
    object Loading : HealthConnectState()

    object Unavailable : HealthConnectState()

    object NeedsInstallation : HealthConnectState()

    data class NeedsPermission(
        val client: HealthConnectClient,
    ) : HealthConnectState()

    data class Ready(
        val client: HealthConnectClient,
    ) : HealthConnectState()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(stateHolder: TrinkAusStateHolder) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var uiState by remember { mutableStateOf<HealthConnectState>(HealthConnectState.Loading) }

    val healthPermissions =
        setOf(
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getWritePermission(HydrationRecord::class),
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
        )

    suspend fun checkAndUpdateState() {
        when (HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata")) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                uiState = HealthConnectState.Unavailable
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                uiState = HealthConnectState.NeedsInstallation
            }
            HealthConnectClient.SDK_AVAILABLE -> {
                val client = HealthConnectClient.getOrCreate(context)
                val granted = client.permissionController.getGrantedPermissions()
                uiState =
                    if (granted.containsAll(healthPermissions)) {
                        // Refresh data if we are moving to the Ready state
                        stateHolder.refreshDataFromSource()
                        HealthConnectState.Ready(client)
                    } else {
                        HealthConnectState.NeedsPermission(client)
                    }
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = PermissionController.createRequestPermissionResultContract(),
        ) {
            // When the user returns from the permission screen, re-check everything.
            // Check after getting permission for newer phones
            scope.launch {
                checkAndUpdateState()
            }
        }

    // Initial check
    LaunchedEffect(Unit) {
        checkAndUpdateState()
    }

    // Check the Health Connect permission state when the app is reopened
    // Check after getting permission for older phones
    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            if (uiState is HealthConnectState.NeedsPermission || uiState is HealthConnectState.NeedsInstallation) {
                scope.launch {
                    checkAndUpdateState()
                }
            }
        }
    }

    // Display the correct UI based on the current HealthConnectState
    when (uiState) {
        is HealthConnectState.Loading -> {
            PermissionLoadingScreen(PaddingValues(0.dp))
        }
        is HealthConnectState.Unavailable -> {
            UnavailableScreen(PaddingValues(0.dp))
        }
        is HealthConnectState.NeedsInstallation -> {
            InstallHealthConnectScreen(PaddingValues(0.dp))
        }
        is HealthConnectState.NeedsPermission -> {
            RequestPermissionScreen(PaddingValues(0.dp)) {
                permissionLauncher.launch(healthPermissions)
            }
        }
        is HealthConnectState.Ready -> {
            MainUi(stateHolder)
        }
    }
}

@Composable
fun UnavailableScreen(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Health Connect is not available on this device.",
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun InstallHealthConnectScreen(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
//                text = stringResource(R.string.health_connect_install_required),
                text = "health_connect_install_required",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
//                text = stringResource(R.string.health_connect_install_description),
                text = "health_connect_install_description",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = {
                    val uriString = "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setPackage("com.android.vending")
                            data = Uri.parse(uriString)
                            putExtra("overlay", true)
                            putExtra("callerId", context.packageName)
                        },
                    )
                },
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
//                    text = stringResource(R.string.install_or_update)
                    text = "install_or_update",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainUi(
    stateHolder: TrinkAusStateHolder,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hydrationLevel = stateHolder.hydrationLevel.collectAsState(0.0)
    val hydrationGoal = stateHolder.hydrationGoal.collectAsState(0.1)
    val largestStreak = stateHolder.largestStreak.collectAsState(StreakResult())
    val currentStreak = stateHolder.currentStreak.collectAsState(StreakResult())
    val useGraphHistory = stateHolder.useGraphHistory.collectAsState(false)
    val showConfetti = remember { mutableStateOf(false) }
    val showSettingsModal = remember { mutableStateOf(false) }
    val isLoading = remember { stateHolder.isLoading }
    val isHideKonfettiEnabled = stateHolder.isHideKonfettiEnabled.collectAsState(false)
    val spacing = 22.dp

    Scaffold(
        modifier = modifier,
    ) { padding ->
        val scrollState = rememberScrollState()

        PullToRefreshBox(
            isRefreshing = isLoading.value,
            onRefresh = {
                stateHolder.refreshDataFromSource()
            },
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
            ) {
                Spacer(modifier = Modifier.height(spacing))

                CurrentHydrationDisplay(
                    hydrationLevel = hydrationLevel.value,
                    hydrationGoal = hydrationGoal.value,
                )

                if (hydrationGoal.value > 0) {
                    Text(
                        text = "${((hydrationLevel.value / hydrationGoal.value) * 100).toInt()}% ${stringResource(R.string.of_goal)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                AddHydrationButtons {
                    showConfetti.value = true
                    stateHolder.addHydration(it)
                    SendMessageThread(context, Constants.Path.UPDATE_HYDRATION, it).start()
                }

                HorizontalDivider()

                StreakDisplay(
                    largestStreak = largestStreak.value,
                    currentStreak = currentStreak.value,
                )

                HistoryMonthSelector(stateHolder)

                if (useGraphHistory.value) {
                    HydrationGraph(stateHolder)
                } else {
                    HydrationCalendar(stateHolder)
                }
            }

            IconButton(
                modifier = Modifier.align(Alignment.TopEnd),
                onClick = { showSettingsModal.value = true },
            ) {
                Icon(
                    imageVector = Lucide.Settings,
                    contentDescription = stringResource(R.string.settings),
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing))

        // ... Konfetti and SettingsPopup logic remains the same ...
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
fun PermissionLoadingScreen(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(padding),
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
    modifier: Modifier = Modifier,
    getPermission: () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(padding),
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
