package com.yukigasai.trinkaus.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
import com.yukigasai.trinkaus.presentation.dataStore
import com.yukigasai.trinkaus.shared.Constants.DataStore.DataStoreKeys
import com.yukigasai.trinkaus.shared.isMetric
import java.time.Instant
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

object HydrationHelper {


    suspend fun getHydrationHistory(context: Context): List<Pair<LocalDateTime, Double>> {
        val now = Instant.now().atZone(ZoneOffset.systemDefault()).toInstant()
        val startOfDay = now.truncatedTo(ChronoUnit.DAYS)
        val timeRangeFilter = TimeRangeFilter.Companion.between(
            startTime = startOfDay, endTime = now
        )

        val readRequest = AggregateGroupByPeriodRequest(
            metrics = setOf(HydrationRecord.Companion.VOLUME_TOTAL),
            timeRangeFilter = timeRangeFilter,
            timeRangeSlicer = Period.ofDays(1),
        )

        val records = HealthConnectClient.Companion.getOrCreate(context).aggregateGroupByPeriod(readRequest)

        return records.map { record ->
            val date = record.startTime
            val volumeInLiters = record.result.doubleValues["Hydration_volume_total"] ?: 0.0
            Pair(date, volumeInLiters)
        }
    }

    suspend fun readHydrationLevel(context: Context): Double {
        try {
            val now = Instant.now().atZone(ZoneId.systemDefault())
            val startOfDay = now.truncatedTo(ChronoUnit.DAYS).toInstant()

            val timeRangeFilter = TimeRangeFilter.Companion.between(
                startTime = startOfDay,
                endTime = now.toInstant()
            )

            val readRequest = ReadRecordsRequest(
                recordType = HydrationRecord::class,
                timeRangeFilter = timeRangeFilter
            )

            // Read the records and calculate the total water intake
            val waterLevel = if (isMetric()) {
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

    suspend fun writeHydrationLevel(context: Context, amount: Double) {
        try {
            val hydrationRecord = HydrationRecord(
                volume = if (isMetric()) Volume.Companion.liters(amount) else Volume.Companion.fluidOuncesUs(
                    amount
                ),
                startTime = Instant.now().minusSeconds(60),
                endTime = Instant.now(),
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                metadata = Metadata.Companion.unknownRecordingMethodWithId("manual")
            )
            HealthConnectClient.Companion.getOrCreate(context).insertRecords(listOf(hydrationRecord))
        } catch (e: Exception) {
            println("Error writing hydration level: ${e.message}")
        }
    }
}