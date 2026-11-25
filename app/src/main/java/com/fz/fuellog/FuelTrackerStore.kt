package com.fz.fuellog

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class FuelEntry(
    val id: Long,
    val timestamp: Long,
    val odometerKm: Double,
    val amountDollars: Double,
    val pricePerGallon: Double,
    val gallons: Double,
    val isFullTank: Boolean,
    val note: String = ""
) {
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

data class TrackerUiState(
    val isTracking: Boolean = false,
    val currentSpeedKmh: Double = 0.0,
    val gpsAccuracyMeters: Float? = null,
    val baseOdometerKm: Double = 1200.0,
    val tripDistanceKm: Double = 0.0,
    val pricePerGallon: Double = 5.13,
    val assumedEfficiencyKmPerGal: Double = 150.0,
    val tankCapacityGallons: Double = 3.17,
    val reserveGallons: Double = 0.63,
    val pendingCycleGallons: Double = 0.0,
    val pendingCycleDollars: Double = 0.0,
    val entries: List<FuelEntry> = emptyList()
) {
    val currentOdometerKm: Double
        get() = baseOdometerKm + tripDistanceKm

    val consumedGallonsEstimate: Double
        get() = if (assumedEfficiencyKmPerGal > 0.0) tripDistanceKm / assumedEfficiencyKmPerGal else 0.0

    val consumedDollarsEstimate: Double
        get() = consumedGallonsEstimate * pricePerGallon

    val remainingGallonsBeforeReserve: Double
        get() {
            val usableTank = max(0.0, tankCapacityGallons - reserveGallons)
            val usedInTank = consumedGallonsEstimate + pendingCycleGallons
            return usableTank - usedInTank
        }

    val estimatedKmBeforeReserve: Double
        get() = remainingGallonsBeforeReserve * assumedEfficiencyKmPerGal
}

class FuelTrackerStore(context: Context) {

    private val prefs = context.getSharedPreferences("fz_fuel_log_prefs", Context.MODE_PRIVATE)

    fun loadState(): TrackerUiState {
        val entriesJson = prefs.getString("entries_json", "[]") ?: "[]"
        val entries = parseEntries(entriesJson)
        return TrackerUiState(
            isTracking = prefs.getBoolean("is_tracking", false),
            currentSpeedKmh = prefs.getFloat("current_speed_kmh", 0f).toDouble(),
            gpsAccuracyMeters = if (prefs.contains("gps_accuracy")) prefs.getFloat("gps_accuracy", 0f) else null,
            baseOdometerKm = prefs.getFloat("base_odometer_km", 1200f).toDouble(),
            tripDistanceKm = prefs.getFloat("trip_distance_km", 0f).toDouble(),
            pricePerGallon = prefs.getFloat("price_per_gallon", 5.13f).toDouble(),
            assumedEfficiencyKmPerGal = prefs.getFloat("efficiency_km_gal", 150f).toDouble(),
            tankCapacityGallons = prefs.getFloat("tank_capacity_gal", 3.17f).toDouble(),
            reserveGallons = prefs.getFloat("reserve_gal", 0.63f).toDouble(),
            pendingCycleGallons = prefs.getFloat("pending_gallons", 0f).toDouble(),
            pendingCycleDollars = prefs.getFloat("pending_dollars", 0f).toDouble(),
            entries = entries
        )
    }

    fun saveState(state: TrackerUiState) {
        val editor = prefs.edit()
            .putBoolean("is_tracking", state.isTracking)
            .putFloat("current_speed_kmh", state.currentSpeedKmh.toFloat())
            .putFloat("base_odometer_km", state.baseOdometerKm.toFloat())
            .putFloat("trip_distance_km", state.tripDistanceKm.toFloat())
            .putFloat("price_per_gallon", state.pricePerGallon.toFloat())
            .putFloat("efficiency_km_gal", state.assumedEfficiencyKmPerGal.toFloat())
            .putFloat("tank_capacity_gal", state.tankCapacityGallons.toFloat())
            .putFloat("reserve_gal", state.reserveGallons.toFloat())
            .putFloat("pending_gallons", state.pendingCycleGallons.toFloat())
            .putFloat("pending_dollars", state.pendingCycleDollars.toFloat())
            .putString("entries_json", serializeEntries(state.entries))

        if (state.gpsAccuracyMeters != null) {
            editor.putFloat("gps_accuracy", state.gpsAccuracyMeters)
        } else {
            editor.remove("gps_accuracy")
        }
        editor.apply()
    }

    fun addGpsDistanceDelta(deltaKm: Double, speedKmh: Double, accuracyMeters: Float?): TrackerUiState {
        val current = loadState()
        val updated = current.copy(
            tripDistanceKm = max(0.0, current.tripDistanceKm + deltaKm),
            currentSpeedKmh = speedKmh,
            gpsAccuracyMeters = accuracyMeters
        )
        saveState(updated)
        return updated
    }

    fun setTrackingActive(active: Boolean): TrackerUiState {
        val current = loadState()
        val updated = current.copy(
            isTracking = active,
            currentSpeedKmh = if (active) current.currentSpeedKmh else 0.0
        )
        saveState(updated)
        return updated
    }

    fun updateManualTelemetry(
        newBaseOdometerKm: Double,
        newTripDistanceKm: Double,
        newPricePerGallon: Double,
        newEfficiencyKmPerGal: Double
    ): TrackerUiState {
        val current = loadState()
        val updated = current.copy(
            baseOdometerKm = max(0.0, newBaseOdometerKm),
            tripDistanceKm = max(0.0, newTripDistanceKm),
            pricePerGallon = if (newPricePerGallon > 0.0) newPricePerGallon else current.pricePerGallon,
            assumedEfficiencyKmPerGal = if (newEfficiencyKmPerGal > 0.0) newEfficiencyKmPerGal else current.assumedEfficiencyKmPerGal
        )
        saveState(updated)
        return updated
    }

    fun registerRefuel(
        odometerKm: Double,
        amountDollars: Double,
        pricePerGallon: Double,
        isFullTank: Boolean,
        note: String = ""
    ): TrackerUiState {
        val current = loadState()
        val safePrice = if (pricePerGallon > 0.0) pricePerGallon else current.pricePerGallon
        val safeDollars = max(0.0, amountDollars)
        val gallons = if (safePrice > 0.0) safeDollars / safePrice else 0.0

        val newEntry = FuelEntry(
            id = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            odometerKm = max(0.0, odometerKm),
            amountDollars = safeDollars,
            pricePerGallon = safePrice,
            gallons = gallons,
            isFullTank = isFullTank,
            note = note.ifBlank { if (isFullTank) "Tanque lleno (Súper)" else "Recarga parcial (Súper)" }
        )

        val updatedEntries = listOf(newEntry) + current.entries
        val totalCycleGallons = current.pendingCycleGallons + gallons
        val totalCycleDollars = current.pendingCycleDollars + safeDollars

        val cycleKm = max(0.0, odometerKm - current.baseOdometerKm)
        val newEfficiency = if (isFullTank && cycleKm > 5.0 && totalCycleGallons > 0.05) {
            val measured = cycleKm / totalCycleGallons
            // Promedio suavizado para evitar picos bruscos en una sola carga
            (current.assumedEfficiencyKmPerGal * 0.4) + (measured * 0.6)
        } else {
            current.assumedEfficiencyKmPerGal
        }

        val updated = if (isFullTank) {
            current.copy(
                baseOdometerKm = max(0.0, odometerKm),
                tripDistanceKm = 0.0,
                pricePerGallon = safePrice,
                assumedEfficiencyKmPerGal = newEfficiency,
                pendingCycleGallons = 0.0,
                pendingCycleDollars = 0.0,
                entries = updatedEntries
            )
        } else {
            current.copy(
                pricePerGallon = safePrice,
                pendingCycleGallons = totalCycleGallons,
                pendingCycleDollars = totalCycleDollars,
                entries = updatedEntries
            )
        }

        saveState(updated)
        return updated
    }

    fun editEntry(
        entryId: Long,
        newOdometerKm: Double,
        newAmountDollars: Double,
        newPricePerGallon: Double,
        newIsFullTank: Boolean,
        newNote: String
    ): TrackerUiState {
        val current = loadState()
        val safePrice = if (newPricePerGallon > 0.0) newPricePerGallon else current.pricePerGallon
        val safeDollars = max(0.0, newAmountDollars)
        val newGallons = if (safePrice > 0.0) safeDollars / safePrice else 0.0

        val editedList = current.entries.map { item ->
            if (item.id == entryId) {
                item.copy(
                    odometerKm = max(0.0, newOdometerKm),
                    amountDollars = safeDollars,
                    pricePerGallon = safePrice,
                    gallons = newGallons,
                    isFullTank = newIsFullTank,
                    note = newNote
                )
            } else {
                item
            }
        }

        val updated = current.copy(
            pricePerGallon = safePrice,
            entries = editedList
        )
        saveState(updated)
        return updated
    }

    fun deleteEntry(entryId: Long): TrackerUiState {
        val current = loadState()
        val updated = current.copy(
            entries = current.entries.filterNot { it.id == entryId }
        )
        saveState(updated)
        return updated
    }

    private fun serializeEntries(entries: List<FuelEntry>): String {
        val array = JSONArray()
        entries.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("timestamp", item.timestamp)
            obj.put("odometerKm", item.odometerKm)
            obj.put("amountDollars", item.amountDollars)
            obj.put("pricePerGallon", item.pricePerGallon)
            obj.put("gallons", item.gallons)
            obj.put("isFullTank", item.isFullTank)
            obj.put("note", item.note)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseEntries(jsonStr: String): List<FuelEntry> {
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<FuelEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FuelEntry(
                        id = obj.optLong("id", System.currentTimeMillis() + i),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        odometerKm = obj.optDouble("odometerKm", 0.0),
                        amountDollars = obj.optDouble("amountDollars", 0.0),
                        pricePerGallon = obj.optDouble("pricePerGallon", 5.13),
                        gallons = obj.optDouble("gallons", 0.0),
                        isFullTank = obj.optBoolean("isFullTank", true),
                        note = obj.optString("note", "Gasolina Súper")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
