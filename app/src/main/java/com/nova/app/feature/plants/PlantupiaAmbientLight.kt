package com.nova.app.feature.plants

/** A bounded session of actual device-reported illuminance events, not a calibrated meter. */
internal data class AmbientLightReading(val lastLux: Float, val minLux: Float, val maxLux: Float,
                                       val count: Int, val lowAccuracy: Boolean, val nearSensorLimit: Boolean)

internal class AmbientLightWindow(private val startedNanos: Long, private val sensorLimit: Float) {
    private var latest: AmbientLightReading? = null
    private var lastNanos = -1L

    fun accept(lux: Float, timestampNanos: Long, accuracy: Int): AmbientLightReading? {
        if (!sensorLimit.isFinite() || sensorLimit <= 0 || !lux.isFinite() || lux < 0 || lux > sensorLimit ||
            timestampNanos < startedNanos || timestampNanos <= lastNanos ||
            timestampNanos - startedNanos > 6_000_000_000L || (latest?.count ?: 0) >= 120) return null
        lastNanos = timestampNanos
        val old = latest
        val value = AmbientLightReading(lux, minOf(lux,old?.minLux ?: lux), maxOf(lux,old?.maxLux ?: lux),
            (old?.count ?: 0)+1, accuracy <= 1 || old?.lowAccuracy == true,
            lux >= .99f*sensorLimit || old?.nearSensorLimit == true)
        latest = value
        return value
    }
}
