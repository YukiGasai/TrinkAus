package com.yukigasai.trinkaus.presentation.settings

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.getDefaultAmount
import com.yukigasai.trinkaus.shared.getDisplayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterAmountSetting(
    sheetState: SheetState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dataStore = DataStoreSingleton.getInstance(context)
    val scope = rememberCoroutineScope()

    val smallAmount by dataStore.data
        .map { it[DataStoreKeys.SMALL_AMOUNT] ?: HydrationOption.SMALL.getDefaultAmount() }
        .collectAsState(initial = null)

    val mediumAmount by dataStore.data
        .map { it[DataStoreKeys.MEDIUM_AMOUNT] ?: HydrationOption.MEDIUM.getDefaultAmount() }
        .collectAsState(initial = null)

    val largeAmount by dataStore.data
        .map { it[DataStoreKeys.LARGE_AMOUNT] ?: HydrationOption.LARGE.getDefaultAmount() }
        .collectAsState(initial = null)

    val amounts =
        listOf(
            HydrationOption.SMALL to smallAmount,
            HydrationOption.MEDIUM to mediumAmount,
            HydrationOption.LARGE to largeAmount,
        )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = spacedBy(16.dp),
    ) {
        amounts.forEach { (hydrationOption, amountState) ->
            if (amountState == null) {
                CircularProgressIndicator()
            } else {
                WaterIntakeItem(
                    hydrationOption = hydrationOption,
                    modifier = Modifier.weight(1f),
                    initialAmount = amountState,
                    sheetState = sheetState,
                    onAmountChange = {
                        scope.launch(Dispatchers.IO) {
                            dataStore.edit { preferences ->
                                preferences[hydrationOption.dataStoreKey] =
                                    it.toIntOrNull()
                                        ?: hydrationOption.getDefaultAmount()
                            }
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterIntakeItem(
    hydrationOption: HydrationOption,
    initialAmount: Int,
    sheetState: SheetState,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var editedAmount by remember(initialAmount) { mutableStateOf(initialAmount.toString()) }
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(initialAmount) {
        if (!isFocused) {
            editedAmount = initialAmount.toString()
        }
    }

    Card(
        modifier = modifier.width(120.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(hydrationOption.icon),
                contentDescription = hydrationOption.getDisplayName(context),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            TextField(
                modifier =
                    Modifier.onFocusChanged { focusState ->
                        isFocused = focusState.isFocused

                        if (isFocused) {
                            scope.launch(Dispatchers.Main) {
                                delay(700)
                                sheetState.expand()
                            }
                        } else {
                            onAmountChange(editedAmount)
                        }
                    },
                value = editedAmount,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        editedAmount = newValue
                    }
                },
                textStyle =
                    TextStyle(
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                    ),
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = Done,
                    ),
                singleLine = true,
                maxLines = 1,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = hydrationOption.getDisplayName(context),
                maxLines = 1,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
