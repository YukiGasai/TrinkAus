package com.yukigasai.trinkaus.presentation.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.util.HydrationHelper

@Composable
fun HealthConnectButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    TextButton(
        onClick = {
            HydrationHelper.openSettings(context)
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
            text = "Health Connect ${stringResource(R.string.settings)}",
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
