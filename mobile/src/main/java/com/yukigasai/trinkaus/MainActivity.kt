package com.yukigasai.trinkaus

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableDoubleState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import com.yukigasai.trinkaus.compose.AddHydrationButtons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var healthConnectClient: HealthConnectClient

    private val hydrationLevel = mutableDoubleStateOf(0.0)
    private val hydrationGoal = mutableDoubleStateOf(0.0)

    val healthPermissions = setOf(
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class)
    )
    val requestPermissionActivityContract =
        PermissionController.createRequestPermissionResultContract()

    private fun getPermissionToPostNotifications() {
        val REQUEST_CODE_POST_NOTIFICATIONS = 1
        if (ContextCompat.checkSelfPermission(
                this, POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(POST_NOTIFICATIONS), REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        healthConnectClient = HealthConnectClient.getOrCreate(this)
        getPermissionToPostNotifications()


        val uri = intent.data
//        if (uri != null) {
//            println("Received hydration message$uri")
//            val hydration = uri.getQueryParameter("hydration")?.toDouble()
//            if (hydration != null) {
//                hydrationLevel.doubleValue = hydration
//            }
//
//            val hydrationGoal = uri.getQueryParameter("hydration_goal")?.toDouble()
//            if (hydrationGoal != null) {
//                this.hydrationGoal.doubleValue = hydrationGoal
//            }
//        }
        println("Received hydration message: $uri")


        val messageFilter = IntentFilter(NEW_HYDRATION)
        val messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                println("Received hydration message after update")
                CoroutineScope(Dispatchers.IO).launch {
                    val hydration = intent.getDoubleExtra(HYDRATION_DATA, 0.0)
                    if (hydration != 0.0) {
                        hydrationLevel.doubleValue = hydration
                    }

                    val newHydrationGoal = intent.getDoubleExtra(HYDRATION_GOAL, 0.0)
                    if (newHydrationGoal != 0.0) {
                        hydrationGoal.doubleValue = newHydrationGoal
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter)

        hydrationGoal.doubleValue = LocalStore.load(this, LocalStore.HYDRATION_GOAL_KEY).toDouble()

        setContent {
            AppTheme {
                MainScreen(hydrationLevel, hydrationGoal)
            }
        }
    }

    private suspend fun checkPermissions(): Boolean {
        val permissions = healthConnectClient.permissionController.getGrantedPermissions()
        return permissions.containsAll(healthPermissions)
    }


    @Composable
    fun MainScreen(hydrationLevel: MutableDoubleState, hydrationGoal: MutableDoubleState) {
        val scope = rememberCoroutineScope()
        val waterLevel = hydrationLevel
        val permissionGranted = remember { mutableStateOf<Boolean?>(null) }
        val context = LocalContext.current
        val showSettingsModal = remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            requestPermissionActivityContract
        ) { grantedPermissions ->
            scope.launch {
                if (checkPermissions()) {
                    println("Permissions granted")
                    permissionGranted.value = true
                    waterLevel.doubleValue = HydrationHelper.readHydrationLevel(context)
                } else {
                    println("Permissions denied")
                    permissionGranted.value = false
                }
            }
        }

        LaunchedEffect(Unit) {
            if (checkPermissions()) {
                permissionGranted.value = true
                waterLevel.doubleValue = HydrationHelper.readHydrationLevel(context)
            } else {
                permissionGranted.value = false
                permissionLauncher.launch(healthPermissions)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .padding(top = 32.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = "Today's Water Intake",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                when (permissionGranted.value) {
                    null -> {
                        CircularProgressIndicator()
                    }

                    false -> {
                        Button(onClick = {
                            println("Requesting permissions")
                            permissionLauncher.launch(healthPermissions)
                        }) {
                            Text("Grant Permissions")
                        }
                    }

                    true -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {

                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.shapes.medium
                                    )
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,


                                ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = getVolumeString(waterLevel.doubleValue),
                                        style = TextStyle(
                                            fontSize = 48.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "/ ${getVolumeStringWithUnit(hydrationGoal.doubleValue)}",
                                        style = TextStyle(
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                CircularProgressIndicator(
                                    progress = { (waterLevel.doubleValue / hydrationGoal.doubleValue).toFloat() },
                                    modifier = Modifier.size(300.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 21.dp,
                                    trackColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                    strokeCap = StrokeCap.Round,
                                    gapSize = 4.dp
                                )
                            }
                            AddHydrationButtons {
                                waterLevel.doubleValue = it
                            }
                        }
                    }
                }

            }

            FloatingActionButton(
                onClick = {
                    showSettingsModal.value = true
                }, modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.glass_small_icon),
                    contentDescription = "Settings"
                )
            }

            if (showSettingsModal.value) {
                Dialog(
                    properties = DialogProperties(
                        dismissOnBackPress = true, dismissOnClickOutside = true
                    ), onDismissRequest = {
                        showSettingsModal.value = false
                    }) {

                    val tmpGoal =
                        remember { mutableDoubleStateOf(hydrationGoal.doubleValue) }

                    Column(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium
                            )
                            .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Set Daily Water Intake Goal",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = getVolumeStringWithUnit(tmpGoal.doubleValue),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Slider(
                            value = tmpGoal.doubleValue.toFloat(),
                            onValueChange = {
                                if (isMetric()) {
                                    tmpGoal.doubleValue = (it * 10).roundToInt() / 10.0
                                } else {
                                    tmpGoal.doubleValue = it.toInt().toDouble()
                                }
                            },
                            valueRange = if (isMetric()) 1f..10f else 1f..200.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                hydrationGoal.doubleValue = tmpGoal.doubleValue
                                LocalStore.save(
                                    context, LocalStore.HYDRATION_GOAL_KEY, tmpGoal.doubleValue
                                )
                                SendMessageThread(
                                    context, "/update_goal", tmpGoal.doubleValue.toString()
                                ).start()
                                showSettingsModal.value = false
                            }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}


//    @Composable
//    fun HydrationBarChart() {
//
//        val context = LocalContext.current
//
//        val dateList = remember { mutableStateListOf<String>() }
//        val dataList = remember { mutableStateListOf<Double>() }
//        val floatValue = remember { mutableStateListOf<Float>() }
//
//        LaunchedEffect(Unit) {
//            val hydrationData = HydrationHelper.getHydrationHistory(context)
//            val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
//
//            hydrationData.forEachIndexed { index, it ->
//                dateList.add(it.first.format(dateTimeFormatter))
//                dataList.add(it.second)
//                floatValue.add(
//                    index = index,
//                    element = (it.second.toFloat() / (hydrationData.maxOfOrNull { it.second }
//                        ?.toFloat() ?: 1.0f)))
//            }
//
//
//        }
//
//
//        Column(
//            modifier = Modifier
//                .padding(horizontal = 30.dp)
//                .fillMaxSize()
//                .background(MaterialTheme.colorScheme.secondaryContainer),
//            verticalArrangement = Arrangement.Center,
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//
//            BarGraph(
//                graphBarData = floatValue,
//                xAxisScaleData = dateList,
//                barData_ = dataList,
//                height = 300.dp,
//                barWidth = 20.dp,
//                barColor = MaterialTheme.colorScheme.primary,
//                barArrangement = Arrangement.SpaceEvenly
//            )
//        }
//    }


class SendMessageThread(
    private val context: Context, private val path: String, private val msg: String
) : Thread() {
    override fun run() {
        try {
            val messageClient = Wearable.getMessageClient(context)
            val wearableList = Wearable.getNodeClient(context).connectedNodes

            val nodes = Tasks.await(wearableList)

            for (node in nodes) {
                val task: Task<Int> = messageClient.sendMessage(node.id, path, msg.toByteArray())
                Tasks.await(task)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

