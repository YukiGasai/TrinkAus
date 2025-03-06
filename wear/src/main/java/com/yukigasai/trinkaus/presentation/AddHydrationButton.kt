package com.yukigasai.trinkaus.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import com.yukigasai.trinkaus.R
import com.yukigasai.trinkaus.util.SendMessageThread
import com.yukigasai.trinkaus.util.getVolumeStringWithUnit
import com.yukigasai.trinkaus.util.isMetric

data class HydrationOption(val icon: Int, val amountUS: Double, val amountMetric: Double)


@Composable
fun AddHydrationButton(
    buttonList: List<HydrationOption>
) {

    val context = LocalContext.current

    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Absolute.SpaceBetween
    ) {
        Spacer(modifier = Modifier.size(8.dp))
        buttonList.forEach {

            val volume = if (isMetric()) it.amountMetric else it.amountUS
            val description = "Add ${getVolumeStringWithUnit(volume)} of water"
            Button(
                modifier = Modifier.size(42.dp),
                onClick = {
                    SendMessageThread(
                        context = context, path = SendMessageThread.ADD_HYDRATION_PATH, msg = volume
                    ).start()
                }) {
                Icon(
                    painter = painterResource(id = it.icon),
                    contentDescription = description
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
    }
}