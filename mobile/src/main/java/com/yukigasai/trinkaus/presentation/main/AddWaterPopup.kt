package com.yukigasai.trinkaus.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.util.HydrationEntry
import com.yukigasai.trinkaus.util.HydrationHelper
import com.yukigasai.trinkaus.util.TrinkAusStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

fun dateToTimeString(date: LocalDateTime): String = "${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWaterPopup(
    stateHolder: TrinkAusStateHolder,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // This is still a good idea as a backup and for handling non-gesture dismiss attempts.
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val scope = rememberCoroutineScope()

    val dataList = remember { mutableStateOf(emptyList<HydrationEntry>()) }

    LaunchedEffect(stateHolder.selectedDate.value) {
        dataList.value =
            HydrationHelper.readHydrationEntriesForDate(
                context,
                stateHolder.selectedDate.value,
            )
    }

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
                text = stringResource(R.string.custom_amount_message),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 64.dp),
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

            HorizontalDivider()

            Text(
                text = stringResource(R.string.entries_for) + " ${stateHolder.selectedDate.value}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )

            if (dataList.value.isNotEmpty()) {
                LazyColumn {
                    items(
                        dataList.value.size,
                        key = { index -> dataList.value[index].uid },
                    ) { index ->
                        val entry = dataList.value[index]
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                ),
                        ) {
                            Text(
                                text = "${dateToTimeString(entry.time)} - ${entry.amount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                            IconButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        HydrationHelper.deleteHydrationEntry(context, entry.uid)
                                        stateHolder.refreshDataFromSource()
                                        sheetState.expand()
                                        dataList.value = dataList.value.filter { it.uid != entry.uid }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = stringResource(R.string.delete_entry),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
