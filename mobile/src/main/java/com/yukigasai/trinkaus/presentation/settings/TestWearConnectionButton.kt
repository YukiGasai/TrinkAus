package com.yukigasai.trinkaus.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.SendMessageResult
import com.yukigasai.trinkaus.shared.WearableMessenger
import kotlinx.coroutines.launch

@Composable
fun TestWearConnectionButton(modifier: Modifier = Modifier) {
    var showNoNodesDialog by remember { mutableStateOf(false) }
    var isWearApiAvailable by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
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

    Column(
        modifier = modifier,
    ) {
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
                            Toast
                                .makeText(
                                    context,
                                    context.getText(R.string.wear_os_test_send_success),
                                    Toast.LENGTH_SHORT,
                                ).show()
                        }
                        is SendMessageResult.NoNodesFound -> showNoNodesDialog = true
                        is SendMessageResult.ApiNotAvailable -> isWearApiAvailable = false
                        is SendMessageResult.Error -> {
                            Toast
                                .makeText(
                                    context,
                                    context.getString(R.string.wear_os_test_send_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
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
                textAlign = TextAlign.Center,
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
                            text = stringResource(R.string.open_play_store),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
