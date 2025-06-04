package com.yukigasai.trinkaus.util

import com.yukigasai.trinkaus.R

data class HydrationOption(
    val icon: Int,
    val amountUS: Double,
    val amountMetric: Double,
) {
    companion object {
        val all =
            listOf(
                HydrationOption(icon = R.drawable.glass_small_icon, amountUS = 5.0, amountMetric = 0.125),
                HydrationOption(icon = R.drawable.glass_icon, amountUS = 9.0, amountMetric = 0.25),
                HydrationOption(icon = R.drawable.bottle_icon, amountUS = 20.0, amountMetric = 0.5),
            )
    }
}
