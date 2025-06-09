package com.yukigasai.trinkaus.util

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.DataStoreSingleton
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

data class StreakResult(
    val length: Int = 0,
    val startDate: LocalDate? = null,
    val isLoading: Boolean = true,
)

val LONG_AGO: LocalDate = LocalDate.of(2010, 1, 1)

object HydrationHelper {
    fun openSettings(context: Context) {
        val intent =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            } else {
                Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

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
                    HealthConnectClient.Companion
                        .getOrCreate(context)
                        .readRecords(readRequest)
                        .records
                        .sumOf {
                            it.volume.inLiters
                        }
                } else {
                    HealthConnectClient.Companion
                        .getOrCreate(context)
                        .readRecords(readRequest)
                        .records
                        .sumOf {
                            it.volume.inFluidOuncesUs
                        }
                }

            val dataStore = DataStoreSingleton.getInstance(context)
            dataStore.edit { preferences ->
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
        amount: Int,
    ) {
        try {
            val hydrationRecord =
                HydrationRecord(
                    volume =
                        if (isMetric()) {
                            Volume.Companion.milliliters(amount.toDouble())
                        } else {
                            Volume.Companion.fluidOuncesUs(
                                amount.toDouble(),
                            )
                        },
                    startTime = Instant.now().minusSeconds(60),
                    endTime = Instant.now(),
                    startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                    endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                    metadata = Metadata.Companion.unknownRecordingMethodWithId("manual"),
                )
            HealthConnectClient.Companion
                .getOrCreate(context)
                .insertRecords(listOf(hydrationRecord))
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

    private suspend fun getAllHydrationHistory(context: Context): Map<LocalDate, Double> {
        try {
            val healthConnectClient = HealthConnectClient.getOrCreate(context)
            val systemZoneId = ZoneId.systemDefault()

            val now = Instant.now()

            val timeRangeFilter =
                TimeRangeFilter.Companion.between(
                    startTime =
                        LONG_AGO
                            .atStartOfDay(systemZoneId)
                            .toInstant(),
                    endTime = now,
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
                // Ensure to use the start time of the record for date grouping
                val recordDate = record.startTime.atZone(systemZoneId).toLocalDate()
                val volume =
                    if (isMetric()) {
                        record.volume.inLiters
                    } else {
                        record.volume.inFluidOuncesUs
                    }
                dailyTotals[recordDate] = (dailyTotals[recordDate] ?: 0.0) + volume
            }
            return dailyTotals
        } catch (e: Exception) {
            println("Error reading all hydration history: ${e.message}")
            return emptyMap()
        }
    }

    suspend fun getLongestWaterIntakeStreak(
        context: Context,
        goal: Double,
    ): StreakResult {
        val allHistoryMap = getAllHydrationHistory(context)
        if (allHistoryMap.isEmpty()) {
            return StreakResult(0, null, isLoading = false)
        }

        // The map keys (LocalDate) are sorted because TreeMap is used.
        val sortedDates = allHistoryMap.keys.toList()

        var longestStreakList = emptyList<HydrationHistoryEntry>()
        var currentStreakList = mutableListOf<HydrationHistoryEntry>()

        var currentDate = sortedDates.first()
        val lastDateWithRecord = sortedDates.last() // Iterate up to the last day with any record

        // Iterate day by day from the first recorded day to the last recorded day
        while (!currentDate.isAfter(lastDateWithRecord)) {
            val intakeForDay = allHistoryMap.getOrDefault(currentDate, 0.0)
            val historyEntryForCurrentDay = HydrationHistoryEntry(currentDate, intakeForDay)

            if (intakeForDay >= goal) {
                currentStreakList.add(historyEntryForCurrentDay)
            } else {
                // Goal not met, or no record for this day (intakeForDay is 0.0 from getOrDefault)
                // Current streak is broken.
                if (currentStreakList.size > longestStreakList.size) {
                    longestStreakList = currentStreakList.toList() // Save if it was the longest
                }
                currentStreakList.clear() // Reset current streak
            }
            currentDate = currentDate.plusDays(1)
        }

        // After the loop, check if the streak that was ongoing at the end is the longest
        if (currentStreakList.size > longestStreakList.size) {
            longestStreakList = currentStreakList.toList()
        }

        return StreakResult(
            length = longestStreakList.size,
            startDate = longestStreakList.lastOrNull()?.date,
            isLoading = false,
        )
    }

    suspend fun getCurrentWaterIntakeStreakLength(
        context: Context,
        goal: Double,
    ): StreakResult {
        val allHistoryMap = getAllHydrationHistory(context)

        var currentStreakLength = 0
        var today = LocalDate.now(ZoneId.systemDefault())
        var checkDate = today
        // Loop backwards
        while (checkDate.isAfter(LONG_AGO)) {
            val intakeForDay = allHistoryMap.getOrDefault(checkDate, 0.0)
            if (intakeForDay >= goal) {
                currentStreakLength++
                checkDate = checkDate.minusDays(1)
            } else {
                // This will make sure the streak is not broken from the start of the current day
                if (checkDate != today) {
                    break
                }
                checkDate = checkDate.minusDays(1)
            }
        }
        checkDate = checkDate.plusDays(1)
        return StreakResult(
            length = currentStreakLength,
            startDate = if (currentStreakLength != 0) checkDate else null,
            isLoading = false,
        )
    }
}
