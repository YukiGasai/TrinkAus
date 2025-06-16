package com.yukigasai.trinkaus.presentation.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TestNotificationButton(
    reminderEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    TextButton(
        enabled = reminderEnabled,
        onClick = {
            scope.launch(Dispatchers.IO) {
                val (currentIntake, percentage) =
                    withContext(Dispatchers.IO) {
                        val dataStore = DataStoreSingleton.getInstance(context)
                        val current = HydrationHelper.readHydrationLevel(context)
                        val goal =
                            dataStore.data.first()[DataStoreKeys.HYDRATION_GOAL] ?: 2.0
                        val percent = if (goal > 0) ((current / goal) * 100).toInt() else 0
                        current to percent
                    }
                NotificationHelper.showNotification(
                    context = context,
                    hydrationLevel = currentIntake,
                    percentage = percentage,
                )
            }
        },
        modifier =
            modifier
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
