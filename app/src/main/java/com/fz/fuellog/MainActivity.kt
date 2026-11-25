package com.fz.fuellog

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var store: FuelTrackerStore
    private var uiState by mutableStateOf(TrackerUiState())

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            uiState = store.loadState()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val fineGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted) {
            startGpsServiceInternal()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = FuelTrackerStore(this)
        uiState = store.loadState()

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AmoledBlack)
            ) {
                FuelDashboardScreen(
                    state = uiState,
                    onToggleTracking = {
                        if (uiState.isTracking) {
                            stopGpsServiceInternal()
                        } else {
                            requestPermissionsAndStartGps()
                        }
                    },
                    onSaveManualEdit = { baseOdo, tripKm, price, eff ->
                        uiState = store.updateManualTelemetry(baseOdo, tripKm, price, eff)
                    },
                    onRegisterRefuel = { odo, dollars, price, isFull, note ->
                        uiState = store.registerRefuel(odo, dollars, price, isFull, note)
                    },
                    onSaveEntryEdit = { id, odo, dollars, price, isFull, note ->
                        uiState = store.editEntry(id, odo, dollars, price, isFull, note)
                    },
                    onDeleteEntry = { id ->
                        uiState = store.deleteEntry(id)
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        uiState = store.loadState()
        val filter = IntentFilter(GpsTrackingService.ACTION_STATE_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stateReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(stateReceiver, filter)
        }
    }

    override fun onStop() {
        unregisterReceiver(stateReceiver)
        super.onStop()
    }

    private fun requestPermissionsAndStartGps() {
        val needed = mutableListOf<String>()
        val hasFine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine) needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!hasCoarse) needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isEmpty()) {
            startGpsServiceInternal()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun startGpsServiceInternal() {
        val intent = Intent(this, GpsTrackingService::class.java)
        ContextCompat.startForegroundService(this, intent)
        uiState = store.setTrackingActive(true)
    }

    private fun stopGpsServiceInternal() {
        val intent = Intent(this, GpsTrackingService::class.java).apply {
            action = GpsTrackingService.ACTION_STOP
        }
        startService(intent)
        uiState = store.setTrackingActive(false)
    }
}
