package com.yukigasai.trinkaus.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.lifecycle.Lifecycle
import com.yukigasai.trinkaus.presentation.main.InstallHealthConnectScreen
import com.yukigasai.trinkaus.presentation.main.MainUI
import com.yukigasai.trinkaus.presentation.main.PermissionLoadingScreen
import com.yukigasai.trinkaus.presentation.main.RequestPermissionScreen
import com.yukigasai.trinkaus.presentation.main.UnavailableScreen
import com.yukigasai.trinkaus.util.OnLifecycleEvent
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.launch

sealed class HealthConnectState {
    data object Loading : HealthConnectState()

    data object Unavailable : HealthConnectState()

    data object NeedsInstallation : HealthConnectState()

    data class NeedsPermission(
        val client: HealthConnectClient,
    ) : HealthConnectState()

    data class Ready(
        val client: HealthConnectClient,
    ) : HealthConnectState()
}

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
            MainUI(stateHolder)
        }
    }
}
