package com.yukigasai.trinkaus.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.util.TrinkAusStateHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWaterPopup(
    stateHolder: TrinkAusStateHolder,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // This is still a good idea as a backup and for handling non-gesture dismiss attempts.
    val sheetState =
        rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.imePadding(),
        dragHandle = null,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Select the custom amount to add",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 32.dp),
            )

            WheelNumberPicker(
                buttonText = stringResource(R.string.add_water),
                onValueChange = { newValue ->
                    stateHolder.addHydration(
                        newValue,
                    )
                    onDismiss()
                },
            )
        }
    }
}
