package com.yukigasai.trinkaus.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yukigasai.trinkaus.R

@Composable
fun RepositoryButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Button(
        modifier =
            modifier.fillMaxWidth(),
        onClick = {
            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/YukiGasai/trinkaus"),
                )
            context.startActivity(intent)
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25292e)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(horizontal = 8.dp)
                    .background(Color.Transparent),
        ) {
            Text(
                text = stringResource(R.string.view_on_github),
                color = Color(0xfafbfcff),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 20.sp),
            )
            Icon(
                painter = painterResource(id = R.drawable.github_logo),
                contentDescription = "GitHub Logo",
                tint = Color(0xfafbfcff),
                modifier =
                    Modifier
                        .size(32.dp)
                        .padding(start = 8.dp),
            )
        }
    }
}
