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
import com.yukigasai.trinkaus.shared.Constants
import com.yukigasai.trinkaus.shared.HydrationOption
import com.yukigasai.trinkaus.shared.SendMessageThread
import com.yukigasai.trinkaus.shared.getDisplayName

@Composable
fun AddHydrationButton(buttonList: List<HydrationOption>) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.size(8.dp))
        buttonList.forEach {
            Button(
                modifier = Modifier.size(42.dp),
                onClick = {
                    SendMessageThread(
                        context = context,
                        path = Constants.Path.ADD_HYDRATION,
                        msg = it,
                    ).start()
                },
            ) {
                Icon(
                    painter = painterResource(id = it.icon),
                    contentDescription = it.getDisplayName(context),
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
    }
}
