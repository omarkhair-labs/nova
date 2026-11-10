package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Shared existing Plantupia delivery contract. QA archive and the one real Shop
 * must collect exactly the same recipient, contact, address and confirmed map pin.
 * No device location is assumed; the existing native MapLibre picker is reused.
 */
internal fun plantupiaDeliveryAddressReady(
    name: String, phone: String, city: String, street: String,
    latitude: Double?, longitude: Double?, pinConfirmed: Boolean,
): Boolean =
    name.trim().length >= 2 && phone.count(Char::isDigit) >= 7 &&
        city.trim().length >= 2 && street.trim().length >= 5 &&
        latitude != null && longitude != null && pinConfirmed &&
        validDeliveryPin(latitude, longitude)

@Composable
internal fun PlantupiaDeliveryAddressFields(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    street: String,
    onStreetChange: (String) -> Unit,
    building: String,
    onBuildingChange: (String) -> Unit,
    landmark: String,
    onLandmarkChange: (String) -> Unit,
    latitude: Double?,
    longitude: Double?,
    pinConfirmed: Boolean,
    onPinConfirmed: (Double, Double) -> Unit,
) {
    var showMap by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        OutlinedTextField(
            value = name, onValueChange = { onNameChange(it.take(160)) },
            label = { Text("Recipient name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = phone, onValueChange = { onPhoneChange(it.take(32)) },
            label = { Text("Phone") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = city, onValueChange = { onCityChange(it.take(120)) },
            label = { Text("City / governorate") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = street, onValueChange = { onStreetChange(it.take(320)) },
            label = { Text("Street / address") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = building, onValueChange = { onBuildingChange(it.take(180)) },
            label = { Text("Building / floor / flat") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = landmark, onValueChange = { onLandmarkChange(it.take(180)) },
            label = { Text("Nearby landmark") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { showMap = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        ) {
            Text(if (pinConfirmed && latitude != null && longitude != null)
                "✓ Exact pin selected · Edit" else "Set exact delivery pin on map")
        }
        if (pinConfirmed && latitude != null && longitude != null) {
            Text(
                "Confirmed pin: %.6f, %.6f".format(
                    java.util.Locale.US, latitude, longitude,
                ),
                style = PlantupiaText.supporting,
            )
        }
        Text(
            "Confirm the entrance and building yourself. The selected map pin is not proof of address accuracy.",
            style = PlantupiaText.supporting,
        )
    }
    if (showMap) {
        PlantupiaDeliveryPinPicker(
            initialLatitude = latitude,
            initialLongitude = longitude,
            onDismiss = { showMap = false },
            onConfirm = { lat, lon ->
                onPinConfirmed(lat, lon)
                showMap = false
            },
        )
    }
}
