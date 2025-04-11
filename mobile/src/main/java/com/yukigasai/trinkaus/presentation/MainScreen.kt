package com.yukigasai.trinkaus.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableDoubleState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import com.yukigasai.trinkaus.shared.getVolumeString
import com.yukigasai.trinkaus.shared.getVolumeStringWithUnit
import com.yukigasai.trinkaus.util.HydrationHelper
import kotlinx.coroutines.launch


private suspend fun checkPermissions(
    healthConnectClient: HealthConnectClient, healthPermissions: Set<String>
): Boolean {
    val permissions = healthConnectClient.permissionController.getGrantedPermissions()
    return permissions.containsAll(healthPermissions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    hydrationLevel: MutableDoubleState,
    hydrationGoal: MutableDoubleState,
    healthConnectClient: HealthConnectClient,
) {
    val scope = rememberCoroutineScope()
    val waterLevel = hydrationLevel
    val permissionGranted = remember { mutableStateOf<Boolean?>(null) }
    val context = LocalContext.current
    val showSettingsModal = remember { mutableStateOf(false) }

    val healthPermissions = setOf(
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class)
    )
    val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    val permissionLauncher = rememberLauncherForActivityResult(
        requestPermissionActivityContract
    ) { grantedPermissions ->
        scope.launch {
            if (checkPermissions(healthConnectClient, healthPermissions)) {
                permissionGranted.value = true
                waterLevel.doubleValue = HydrationHelper.readHydrationLevel(context)
            } else {
                permissionGranted.value = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (checkPermissions(healthConnectClient, healthPermissions)) {
            permissionGranted.value = true
            waterLevel.doubleValue = HydrationHelper.readHydrationLevel(context)
        } else {
            permissionGranted.value = false
            permissionLauncher.launch(healthPermissions)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text = "Today's Water Intake",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                ) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSettingsModal.value = true },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                text = { Text("Settings") },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        }
    ) { padding ->
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
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                when (permissionGranted.value) {
                    null -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Checking permissions...",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    false -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Permissions Required",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Please grant access to Health Connect to track your water intake",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                FilledTonalButton(
                                    onClick = { permissionLauncher.launch(healthPermissions) },
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Text("Grant Permissions")
                                }
                            }
                        }
                    }
                    true -> {
                        // Water intake card with progress
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
                                    progress = { (waterLevel.doubleValue / hydrationGoal.doubleValue).coerceIn(0.0, 1.0)
                                        .toFloat() },
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
                                        text = getVolumeString(waterLevel.doubleValue),
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "/${getVolumeStringWithUnit(hydrationGoal.doubleValue)}",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }

                        // Percentage indicator
                        Text(
                            text = "${((waterLevel.doubleValue / hydrationGoal.doubleValue) * 100).toInt()}% of goal",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        AddHydrationButtons { waterLevel.doubleValue = it }
                    }
                }
            }
        }

        if (showSettingsModal.value) {
            SettingsPopup(
                hydrationGoal = hydrationGoal.doubleValue,
                updateHydrationGoal = { hydrationGoal.doubleValue = it },
                updateShowSettingsModal = { showSettingsModal.value = it },
            )
        }
    }
}