package com.yukigasai.trinkaus.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.wheel_picker.FVerticalWheelPicker
import com.sd.lib.compose.wheel_picker.rememberFWheelPickerState

@Composable
fun WheelNumberPicker(
    onValueChange: (Int) -> Unit,
    buttonText: String,
    modifier: Modifier = Modifier,
) {
    val state0 =
        rememberFWheelPickerState(
            initialIndex = 0,
        )
    val state1 =
        rememberFWheelPickerState(
            initialIndex = 0,
        )
    val state2 =
        rememberFWheelPickerState(
            initialIndex = 0,
        )
    val state3 =
        rememberFWheelPickerState(
            initialIndex = 0,
        )

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            FVerticalWheelPicker(
                modifier = Modifier.weight(1f),
                count = 10,
                state = state0,
                key = { it },
                unfocusedCount = 1,
                itemHeight = 52.dp,
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    text = it.toString(),
                )
            }
            FVerticalWheelPicker(
                modifier = Modifier.weight(1f),
                count = 10,
                state = state1,
                key = { it },
                unfocusedCount = 1,
                itemHeight = 52.dp,
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    text = it.toString(),
                )
            }
            FVerticalWheelPicker(
                modifier = Modifier.weight(1f),
                count = 10,
                state = state2,
                key = { it },
                unfocusedCount = 1,
                itemHeight = 52.dp,
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    text = it.toString(),
                )
            }
            FVerticalWheelPicker(
                modifier = Modifier.weight(1f),
                count = 10,
                state = state3,
                key = { it },
                unfocusedCount = 1,
                itemHeight = 52.dp,
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    text = it.toString(),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                val newValue =
                    state0.currentIndex * 1000 + state1.currentIndex * 100 + state2.currentIndex * 10 + state3.currentIndex
                onValueChange(newValue)
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
                text = buttonText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
