package com.nova.app.feature.plants

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
internal fun PlantupiaAmbientLightCheck() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val manager = remember(context) { context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }
    val sensor = remember(manager) { manager?.getDefaultSensor(Sensor.TYPE_LIGHT) }
    HorizontalDivider(color = PlantupiaColors.border)
    Text("Check actual light", style = PlantupiaText.editorialHeading)
    if (manager == null || sensor == null || !sensor.maximumRange.isFinite() || sensor.maximumRange <= 0f) {
        Text("A compatible ambient-light sensor is not available on this phone. The simulation is still available.",
            style = PlantupiaText.supporting)
        return
    }
    var running by remember(owner,sensor) { mutableStateOf(false) }
    var reading by remember(owner,sensor) { mutableStateOf<AmbientLightReading?>(null) }
    var message by remember(owner,sensor) { mutableStateOf("") }
    Text("Hold the phone at the point you want to check, with the light sensor uncovered. This is a local device reading, not light absorbed by the plant.",
        style = PlantupiaText.supporting)

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                running = false
                reading = null
                message = ""
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    if (running) {
        DisposableEffect(owner,manager,sensor) {
            val window = AmbientLightWindow(SystemClock.elapsedRealtimeNanos(),sensor.maximumRange)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_LIGHT) {
                        val value = event.values.firstOrNull() ?: return
                        if (running) window.accept(value,event.timestamp,event.accuracy)?.let { reading = it }
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            val registered = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                runCatching { manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_NORMAL) }.getOrDefault(false)
            if (!registered) {
                message = "The light sensor could not start."
                running = false
            }
            onDispose { if (registered) manager.unregisterListener(listener) }
        }
        LaunchedEffect(owner,sensor) {
            delay(6000)
            running = false
            if (reading == null && message.isBlank()) message = "No fresh sensor reading was received. Try again."
        }
    }
    reading?.let {
        Text(String.format(Locale.ROOT,"%.1f lx",it.lastLux),style = PlantupiaText.display)
        Text(if (running) "Latest device event" else "Last device event during this check",style = PlantupiaText.itemTitle)
        if (it.count > 1) Text(String.format(Locale.ROOT,"Reported range %.1f–%.1f lx · %d events",it.minLux,it.maxLux,it.count),
            style = PlantupiaText.supporting)
        if (it.lowAccuracy || it.nearSensorLimit) Text("The device indicates low accuracy or a reading near its sensor limit. Treat this reading cautiously.",
            style = PlantupiaText.supporting)
    }
    if (message.isNotBlank()) Text(message,style = PlantupiaText.supporting)
    TextButton(onClick = {
        if (running) {
            running = false
            if (reading == null) message = "No fresh sensor reading was received."
        } else {
            reading = null
            message = ""
            running = true
        }
    }) { Text(if (running) "Stop light check" else "Read phone light sensor") }
    Text("Uncalibrated, temporary lux reading · not saved or shared. No PAR, daily light integral or suitability is inferred.",
        style = PlantupiaText.supporting)
}
