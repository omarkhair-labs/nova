package com.nova.app.feature.plants

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val START_LATITUDE = 30.0444 // Cairo is a starting viewport, NOT the user's location.
private const val START_LONGITUDE = 31.2357

internal fun validDeliveryPin(latitude: Double, longitude: Double): Boolean =
    latitude.isFinite() && longitude.isFinite() &&
        latitude in -90.0..90.0 && longitude in -180.0..180.0

private fun lastPermittedLocation(context: Context): Location? {
    val permissionGranted = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    if (!permissionGranted) return null
    return try {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider ->
                if (manager.isProviderEnabled(provider)) manager.getLastKnownLocation(provider) else null
            }.maxByOrNull { it.time }
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * Interactive native map; user must confirm the pin. No background tracking, address guessing
 * or automatic consent. If maps fail, the QA checkout stays unsubmitted, not falsely located.
 */
@Composable
internal fun PlantupiaDeliveryPinPicker(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val startLat = initialLatitude?.takeIf { validDeliveryPin(it, initialLongitude ?: 0.0) } ?: START_LATITUDE
    val startLon = initialLongitude?.takeIf { validDeliveryPin(startLat, it) } ?: START_LONGITUDE
    var latitude by remember { mutableStateOf(startLat) }
    var longitude by remember { mutableStateOf(startLon) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("Drag the map to place the pin exactly at your delivery entrance.") }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }

    fun centerOnDeviceLocation() {
        val location = lastPermittedLocation(context)
        if (location == null) {
            feedback = "Location is unavailable. Move the map manually to choose your delivery point."
            return
        }
        latitude = location.latitude
        longitude = location.longitude
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 16.0))
        feedback = "Suggested from your device. Adjust the pin to the building entrance and confirm."
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) centerOnDeviceLocation()
        else feedback = "You can choose your delivery pin manually without location permission."
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val listener = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(listener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(listener)
            runCatching { mapView.onPause() }
            runCatching { mapView.onStop() }
            mapView.onDestroy()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFFFDF7)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Delivery location", style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFF123B2E))
                    Text(feedback, style = MaterialTheme.typography.bodySmall)
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
                    Text("📍", modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.headlineLarge)
                    if (!mapReady) Text(
                        "Loading map…", modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selected pin: %.6f, %.6f".format(java.util.Locale.US, latitude, longitude),
                        style = MaterialTheme.typography.bodySmall)
                    Text("Map data © OpenStreetMap contributors · OpenFreeMap",
                        style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                            val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION).any {
                                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                            }
                            if (granted) centerOnDeviceLocation()
                            else locationPermission.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ))
                        }) { Text("My location") }
                        OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                    }
                    Button(
                        onClick = {
                            if (validDeliveryPin(latitude, longitude)) onConfirm(latitude, longitude)
                        },
                        enabled = mapReady && validDeliveryPin(latitude, longitude),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF123B2E)),
                    ) { Text("Confirm exact delivery pin") }
                }
            }
        }
    }

    DisposableEffect(mapView) {
        mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(startLat, startLon)).zoom(14.0).build()
            readyMap.addOnCameraIdleListener {
                val center = readyMap.cameraPosition.target
                latitude = center.latitude
                longitude = center.longitude
            }
            readyMap.setStyle(MAP_STYLE) {
                mapReady = true
            }
        }
        onDispose { map = null }
    }
}
