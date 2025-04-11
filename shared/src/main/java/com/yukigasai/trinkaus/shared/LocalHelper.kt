package com.yukigasai.trinkaus.shared

import android.icu.util.LocaleData
import android.icu.util.ULocale

fun isMetric(): Boolean {
    val measureMentSystem = LocaleData.getMeasurementSystem(ULocale.getDefault())
    return measureMentSystem == LocaleData.MeasurementSystem.SI
}

fun getUnit(): String {
    return if (isMetric()) "L" else "fl oz"
}

fun getVolumeString(volume: Double): String {
    return String
        .format("%.3f", volume)
        .trimEnd('0')
        .trimEnd('.')
        .trimEnd(',')
}

fun getVolumeStringWithUnit(volume: Double): String {
    return "${getVolumeString(volume)} ${getUnit()}"
}