package com.yukigasai.trinkaus.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import com.yukigasai.trinkaus.presentation.dataStore
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.isMetric
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.TreeMap

data class HydrationHistoryEntry(
    val date: LocalDate,
    val amount: Double,
)

object HydrationHelper {
    suspend fun readHydrationLevel(context: Context): Double {
        try {
            val now = Instant.now().atZone(ZoneId.systemDefault())
            val startOfDay = now.truncatedTo(ChronoUnit.DAYS).toInstant()

            val timeRangeFilter =
                TimeRangeFilter.Companion.between(
                    startTime = startOfDay,
                    endTime = now.toInstant(),
                )

            val readRequest =
                ReadRecordsRequest(
                    recordType = HydrationRecord::class,
                    timeRangeFilter = timeRangeFilter,
                )

            // Read the records and calculate the total water intake
            val waterLevel =
                if (isMetric()) {
                    HealthConnectClient.Companion.getOrCreate(context).readRecords(readRequest).records.sumOf {
                        it.volume.inLiters
                    }
                } else {
                    HealthConnectClient.Companion.getOrCreate(context).readRecords(readRequest).records.sumOf {
                        it.volume.inFluidOuncesUs
                    }
                }

            context.dataStore.edit { preferences ->
                preferences[DataStoreKeys.HYDRATION_LEVEL] = waterLevel
            }

            return waterLevel
        } catch (e: Exception) {
            // Handle other exceptions
            println("Error reading hydration level: ${e.message}")
            return 0.0
        }
    }

    suspend fun writeHydrationLevel(
        context: Context,
        amount: Double,
    ) {
        try {
            val hydrationRecord =
                HydrationRecord(
                    volume =
                        if (isMetric()) {
                            Volume.Companion.liters(amount)
                        } else {
                            Volume.Companion.fluidOuncesUs(
                                amount,
                            )
                        },
                    startTime = Instant.now().minusSeconds(60),
                    endTime = Instant.now(),
                    startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                    endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                    metadata = Metadata.Companion.unknownRecordingMethodWithId("manual"),
                )
            HealthConnectClient.Companion.getOrCreate(context).insertRecords(listOf(hydrationRecord))
        } catch (e: Exception) {
            println("Error writing hydration level: ${e.message}")
        }
    }

    suspend fun getHydrationHistoryForMonth(
        context: Context,
        dateInMonth: LocalDate,
    ): Map<LocalDate, Double> {
        val year = dateInMonth.year
        val month = dateInMonth.monthValue

        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(context)
            val systemZoneId = ZoneId.systemDefault()

            val yearMonth = YearMonth.of(year, month)
            val startOfMonth = yearMonth.atDay(1)
            val startOfNextMonth = yearMonth.plusMonths(1).atDay(1)

            val startOfMonthInstant = startOfMonth.atStartOfDay(systemZoneId).toInstant()
            val endOfMonthInstant = startOfNextMonth.atStartOfDay(systemZoneId).toInstant()

            val timeRangeFilter =
                TimeRangeFilter.between(
                    startTime = startOfMonthInstant,
                    endTime = endOfMonthInstant,
                )

            val request =
                ReadRecordsRequest(
                    recordType = HydrationRecord::class,
                    timeRangeFilter = timeRangeFilter,
                )

            val records = healthConnectClient.readRecords(request).records

            if (records.isEmpty()) {
                return emptyMap()
            }

            val dailyTotals = TreeMap<LocalDate, Double>()

            for (record in records) {
                val recordDate = record.startTime.atZone(systemZoneId).toLocalDate()
                if (recordDate.year == year && recordDate.monthValue == month) {
                    val volume =
                        if (isMetric()) {
                            record.volume.inLiters
                        } else {
                            record.volume.inFluidOuncesUs
                        }
                    dailyTotals[recordDate] = (dailyTotals[recordDate] ?: 0.0) + volume
                }
            }

            for (day in 1..yearMonth.lengthOfMonth()) {
                val date = LocalDate.of(year, month, day)
                dailyTotals.putIfAbsent(date, 0.0)
            }

            return dailyTotals
        } catch (e: Exception) {
            println("Error reading hydration history for month $year-$month: ${e.message}")
            return emptyMap()
        }
    }
}
