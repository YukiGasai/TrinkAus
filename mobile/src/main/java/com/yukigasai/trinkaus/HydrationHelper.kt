package com.yukigasai.trinkaus

import android.content.Context
import android.icu.util.LocaleData
import android.icu.util.ULocale
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Volume
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
        val timeRangeFilter = TimeRangeFilter.between(
            startTime = startOfDay, endTime = now
        )

        val readRequest = AggregateGroupByPeriodRequest(
            metrics = setOf(HydrationRecord.VOLUME_TOTAL),
            timeRangeFilter = timeRangeFilter,
            timeRangeSlicer = Period.ofDays(1),
        )

        val records = HealthConnectClient.getOrCreate(context).aggregateGroupByPeriod(readRequest)

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

            val timeRangeFilter = TimeRangeFilter.between(
                startTime = startOfDay,
                endTime = now.toInstant()
            )

            val readRequest = ReadRecordsRequest(
                recordType = HydrationRecord::class,
                timeRangeFilter = timeRangeFilter
            )

            // Read the records and calculate the total water intake
            val waterLevel = if (isMetric()) {
                HealthConnectClient.getOrCreate(context).readRecords(readRequest).records.sumOf {
                    it.volume.inLiters
                }
            } else {
                HealthConnectClient.getOrCreate(context).readRecords(readRequest).records.sumOf {
                    it.volume.inFluidOuncesUs
                }
            }

            return waterLevel
        }catch(e: Exception){
            return 0.0
        }
    }

    suspend fun writeHydrationLevel(context: Context, amount: Double) {
        val hydrationRecord = HydrationRecord(
            volume = if(isMetric()) Volume.liters(amount) else Volume.fluidOuncesUs(amount),
            startTime = Instant.now().minusSeconds(60),
            endTime = Instant.now(),
            startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
            endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
            metadata = Metadata.unknownRecordingMethodWithId("manual")
        )

        HealthConnectClient.getOrCreate(context).insertRecords(listOf(hydrationRecord))
    }
}