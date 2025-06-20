package com.yukigasai.trinkaus.presentation.settings

import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Absolute.SpaceBetween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.service.WaterServerService
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.util.OnLifecycleEvent
import com.yukigasai.trinkaus.util.ServerManager
import com.yukigasai.trinkaus.util.ServerManager.getServerUrl
import com.yukigasai.trinkaus.util.ServerManager.startServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun WaterServiceSettings(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataStore = DataStoreSingleton.getInstance(context)
    val scope = rememberCoroutineScope()
    val requestedPermission = remember { mutableStateOf(false) }

    fun isExactAlarmPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val alarmManager = getSystemService(context, AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    OnLifecycleEvent { _, event ->
        if (event == Lifecycle.Event.ON_RESUME && requestedPermission.value) {
            requestedPermission.value = false
            if (isExactAlarmPermissionGranted()) {
                scope.launch(Dispatchers.IO) {
                    dataStore.edit { preferences ->
                        preferences[DataStoreKeys.USE_LOCAL_SERVER] = true
                    }
                    startServer(context)
                }
            } else {
                Toast
                    .makeText(
                        context,
                        "Couldn't start server. Please allow exact alarm permission in settings.",
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    val useLocalServer by dataStore.data
        .map { it[DataStoreKeys.USE_LOCAL_SERVER] == true }
        .collectAsState(initial = false)

    val authToken by dataStore.data
        .map { it[DataStoreKeys.AUTH_TOKEN] ?: "" }
        .collectAsState(initial = "")

    var serverIp by remember { mutableStateOf("") }
    var serverIpError by remember { mutableStateOf(false) }

    fun startServer() {
        val serviceIntent =
            Intent(context, WaterServerService::class.java).apply {
                action = WaterServerService.ACTION_START
            }
        context.startForegroundService(serviceIntent)

        scope.launch {
            val authToken =
                dataStore.data
                    .firstOrNull()
                    ?.get(DataStoreKeys.AUTH_TOKEN)

            if (authToken.isNullOrEmpty()) {
                ServerManager.createOrRefreshAuthToken(context)
            }
        }
    }

    fun stopServer() {
        val serviceIntent = Intent(context, WaterServerService::class.java)
        context.stopService(serviceIntent)
    }

    LaunchedEffect(useLocalServer) {
        val ip = getServerUrl()
        if (ip.isSuccess) {
            serverIpError = false
            serverIp = ip.getOrNull() ?: context.getString(R.string.unknown_error)
        } else {
            serverIpError = true
            serverIp = ip.exceptionOrNull()?.message ?: context.getString(R.string.unknown_error)
        }
    }

    OptionSection(
        modifier = modifier,
        headerTitle = stringResource(R.string.local_api_server),
        headerContent = {
            Switch(
                checked = useLocalServer,
                onCheckedChange = { isChecked ->
                    if (isChecked && !isExactAlarmPermissionGranted()) {
                        requestedPermission.value = true
                        context.startActivity(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        return@Switch
                    }

                    scope.launch(Dispatchers.IO) {
                        dataStore.edit { preferences ->
                            preferences[DataStoreKeys.USE_LOCAL_SERVER] = isChecked
                        }
                        if (isChecked) {
                            startServer()
                        } else {
                            stopServer()
                        }
                    }
                },
            )
        },
    ) {
        SettingsSubTitle(
            stringResource(R.string.ip_address),
        )

        Text(
            text = serverIp,
            modifier = Modifier.fillMaxWidth().copyableOnLongClick(context, serverIp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (serverIpError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )

        SettingsSubTitle(
            stringResource(R.string.authentication_token),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = authToken,
                modifier = Modifier.weight(1f).copyableOnLongClick(context, authToken),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            IconButton(
                enabled = useLocalServer,
                onClick = {
                    scope.launch {
                        ServerManager.createOrRefreshAuthToken(context)
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_refresh_24),
                    contentDescription = "Refresh Auth Token",
                    tint =
                        if (useLocalServer) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.12f,
                            )
                        },
                )
            }
        }
    }
}

fun Modifier.copyableOnLongClick(
    context: Context,
    text: String,
): Modifier =
    this.clickable {
        val clipboard = getSystemService(context, ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.copy_text_title), text))
        Toast.makeText(context, context.getString(R.string.copy_text_description), Toast.LENGTH_SHORT).show()
    }
