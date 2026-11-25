package com.fz.fuellog

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class GpsTrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var store: FuelTrackerStore
    private var lastValidLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            processLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        store = FuelTrackerStore(this)
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTracking()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification(store.loadState()))
                store.setTrackingActive(true)
                requestLocationUpdates()
                return START_STICKY
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        fusedClient.removeLocationUpdates(locationCallback)
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun processLocation(location: Location) {
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        val rawSpeedKmh = if (location.hasSpeed()) {
            (location.speed * 3.6).toDouble()
        } else {
            0.0
        }
        val filteredSpeedKmh = if (rawSpeedKmh < 2.0) 0.0 else rawSpeedKmh

        var deltaKm = 0.0
        val prev = lastValidLocation
        if (prev != null) {
            val distanceMeters = prev.distanceTo(location)
            // Filtra saltos erróneos de GPS o poca precisión
            val validAccuracy = accuracy == null || accuracy <= 35f
            if (validAccuracy && distanceMeters in 3.0f..250.0f) {
                deltaKm = distanceMeters / 1000.0
            }
        }

        if (accuracy == null || accuracy <= 50f) {
            lastValidLocation = location
        }

        val updatedState = store.addGpsDistanceDelta(
            deltaKm = deltaKm,
            speedKmh = filteredSpeedKmh,
            accuracyMeters = accuracy
        )

        updateNotification(updatedState)
        sendBroadcast(Intent(ACTION_STATE_UPDATED).setPackage(packageName))
    }

    private fun stopTracking() {
        fusedClient.removeLocationUpdates(locationCallback)
        lastValidLocation = null
        store.setTrackingActive(false)
        sendBroadcast(Intent(ACTION_STATE_UPDATED).setPackage(packageName))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(state: TrackerUiState) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: TrackerUiState): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, GpsTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = String.format(
            Locale.US,
            "%.1f km | %.2f gal Súper ($%.2f) | %.0f km/h",
            state.tripDistanceKm,
            state.consumedGallonsEstimate,
            state.consumedDollarsEstimate,
            state.currentSpeedKmh
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FZ 2.0 Súper — Rastreo GPS activo")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", pendingStop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rastreo GPS Yamaha FZ 2.0",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "fz_gps_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.fz.fuellog.action.STOP_TRACKING"
        const val ACTION_STATE_UPDATED = "com.fz.fuellog.action.STATE_UPDATED"
    }
}
