package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

internal fun normalizedDeliveryPhone(input: String): String =
    input.trim().map { character ->
        character.digitToIntOrNull()?.digitToChar() ?: character
    }.joinToString("")

private val deliveryPhonePattern = Regex("[+0-9 ()-]{7,32}")

/** Shared existing Plantupia delivery contract. QA archive and the one real Shop
 * must collect exactly the same recipient, contact, address and confirmed map pin.
 * No device location is assumed; the existing native MapLibre picker is reused.
 */
internal fun plantupiaDeliveryAddressReady(
    name: String, phone: String, city: String, street: String,
    latitude: Double?, longitude: Double?, pinConfirmed: Boolean,
): Boolean =
    name.trim().length >= 2 &&
        normalizedDeliveryPhone(phone).let { deliveryPhonePattern.matches(it) && it.count(Char::isDigit) >= 7 } &&
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
    showErrors: Boolean = false,
) {
    var showMap by remember { mutableStateOf(false) }

    val phoneNormalized = normalizedDeliveryPhone(phone)
    val phoneInvalid = !deliveryPhonePattern.matches(phoneNormalized) ||
        phoneNormalized.count(Char::isDigit) < 7
    val pinValid = latitude != null && longitude != null && pinConfirmed &&
        validDeliveryPin(latitude, longitude)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text("WHO CAN RECEIVE IT", style = PlantupiaText.sectionLabel)
        OutlinedTextField(
            value = name, onValueChange = { onNameChange(it.take(160)) },
            label = { Text("Recipient name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = showErrors && name.trim().length < 2,
        )
        if (showErrors && name.trim().length < 2) {
            Text("Enter the recipient's name.", style = PlantupiaText.supporting,
                color = PlantupiaColors.errorText)
        }
        OutlinedTextField(
            value = phone, onValueChange = { onPhoneChange(it.take(32)) },
            label = { Text("Phone number") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            isError = showErrors && phoneInvalid,
        )
        if (showErrors && phoneInvalid) {
            Text("Enter a reachable phone number with at least seven digits.",
                style = PlantupiaText.supporting, color = PlantupiaColors.errorText)
        }

        HorizontalDivider(color = PlantupiaColors.border)
        Text("THE DELIVERY PLACE", style = PlantupiaText.sectionLabel)
        OutlinedTextField(
            value = city, onValueChange = { onCityChange(it.take(120)) },
            label = { Text("City / governorate") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = showErrors && city.trim().length < 2,
        )
        if (showErrors && city.trim().length < 2) {
            Text("Enter the city or governorate.", style = PlantupiaText.supporting,
                color = PlantupiaColors.errorText)
        }
        OutlinedTextField(
            value = street, onValueChange = { onStreetChange(it.take(320)) },
            label = { Text("Street / delivery address") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2, maxLines = 3,
            isError = showErrors && street.trim().length < 5,
        )
        if (showErrors && street.trim().length < 5) {
            Text("Add the street and details needed to locate the entrance.",
                style = PlantupiaText.supporting, color = PlantupiaColors.errorText)
        }
        OutlinedTextField(
            value = building, onValueChange = { onBuildingChange(it.take(180)) },
            label = { Text("Building / floor / flat (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = landmark, onValueChange = { onLandmarkChange(it.take(180)) },
            label = { Text("Nearby landmark (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider(color = PlantupiaColors.border)
        Text("THE EXACT PIN", style = PlantupiaText.sectionLabel)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PlantupiaColors.softSage,
            border = androidx.compose.foundation.BorderStroke(1.dp, PlantupiaColors.border),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (pinValid) "Pin selected by you" else "Mark the actual entrance",
                    style = PlantupiaText.body,
                )
                Text(
                    "Use the native map to confirm your delivery point. " +
                        "No device location is used without your permission.",
                    style = PlantupiaText.supporting,
                )
                OutlinedButton(
                    onClick = { showMap = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text(if (pinValid) "✓ Edit confirmed map pin" else "Open delivery map  ↗")
                }
                if (pinValid) {
                    Text(
                        "Pin: %.6f, %.6f".format(
                            java.util.Locale.US, latitude, longitude,
                        ),
                        style = PlantupiaText.sectionLabel,
                    )
                }
                if (showErrors && !pinValid) {
                    Text(
                        "Confirm the map pin for this address before continuing.",
                        color = PlantupiaColors.errorText, style = PlantupiaText.supporting,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                Text(
                    "A pin is your selected location, not proof of the address or entrance.",
                    style = PlantupiaText.supporting,
                )
            }
        }
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
