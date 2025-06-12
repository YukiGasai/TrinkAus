package com.yukigasai.trinkaus.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.R

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
