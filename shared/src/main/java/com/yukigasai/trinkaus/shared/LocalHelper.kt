package com.yukigasai.trinkaus.shared

import android.icu.util.LocaleData
import android.icu.util.ULocale

fun isMetric(): Boolean {
    val measureMentSystem = LocaleData.getMeasurementSystem(ULocale.getDefault())
    return measureMentSystem == LocaleData.MeasurementSystem.SI
}

fun getUnit(): String = if (isMetric()) "L" else "fl oz"

fun getVolumeString(volume: Double): String =
    String
        .format("%.3f", volume)
        .trimEnd('0')
        .trimEnd('.')
        .trimEnd(',')

fun getVolumeStringWithUnit(volume: Double): String = "${getVolumeString(volume)} ${getUnit()}"
