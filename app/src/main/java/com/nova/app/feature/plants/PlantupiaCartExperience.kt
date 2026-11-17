package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.data.PlantupiaRepository
import org.json.JSONObject

/** Chapter 04 / a reviewable saved collection, not a checkout or stock hold. */
@Composable
internal fun PlantupiaCartExperience(
    cart: JSONObject,
    repository: PlantupiaRepository,
    busy: Boolean,
    pendingSku: String?,
    removed: ShopRemovedCartObject?,
    error: String?,
    notice: String?,
    onQuantityChange: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onRestore: () -> Unit,
    onRefresh: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onContinueShopping: () -> Unit,
    onDeliveryDetails: () -> Unit,
) {
    val objects = shopCartObjects(cart)
    val serverCount = cart.optJSONArray("results")?.length() ?: -1
    val incomplete = serverCount != objects.size
    val units = objects.sumOf { it.quantity }
    val isActionBlocked = busy || error != null

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(19.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("THE SAVED COLLECTION / 04", style = PlantupiaText.sectionLabel)
                Text(
                    if (objects.isEmpty()) "Nothing saved yet"
                    else "${objects.size} ${if (objects.size == 1) "object" else "objects"} · $units ${if (units == 1) "unit" else "units"}",
                    style = PlantupiaText.body, fontWeight = FontWeight.SemiBold,
                )
            }
            if (objects.isNotEmpty()) {
                TextButton(onClick = onContinueShopping, enabled = !busy) {
                    Text("Explore more  ↗", color = PlantupiaColors.forest)
                }
            }
        }

        if (error != null || incomplete) {
            Surface(
                color = PlantupiaColors.paper,
                border = BorderStroke(1.dp, PlantupiaColors.errorText),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        error ?: "Some saved items could not be read. Refresh the real cart before continuing.",
                        color = PlantupiaColors.errorText,
                        style = PlantupiaText.body,
                    )
                    TextButton(onClick = onRefresh, enabled = !busy) {
                        Text("Refresh saved cart  ↗", color = PlantupiaColors.forest)
                    }
                }
            }
        } else if (notice != null) {
            Text(
                notice, style = PlantupiaText.supporting,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (removed != null) {
            Surface(
                color = PlantupiaColors.softSage,
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Removed from your saved cart", style = PlantupiaText.body,
                        fontWeight = FontWeight.SemiBold)
                    Text(removed.name + " · " + removed.previousQuantity + " " +
                        if (removed.previousQuantity == 1) "unit" else "units",
                        style = PlantupiaText.supporting)
                    TextButton(onClick = onRestore, enabled = !isActionBlocked) {
                        Text("Restore saved selection  ↗", color = PlantupiaColors.forest)
                    }
                    Text("Restoring rechecks current availability and may be refused.",
                        style = PlantupiaText.supporting)
                }
            }
        }

        if (objects.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("A space takes shape one object at a time.",
                    style = PlantupiaText.editorialHeading)
                Text("Nothing is reserved or ordered here yet.",
                    style = PlantupiaText.supporting)
                OutlinedButton(
                    onClick = onContinueShopping,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("Explore the living collection  ↗") }
            }
        } else {
            objects.forEachIndexed { position, item ->
                key(item.skuId) {
                    PlantupiaCartObjectCard(
                        item = item,
                        index = position + 1,
                        repository = repository,
                        pending = pendingSku == item.skuId,
                        enabled = !isActionBlocked,
                        onDecrease = { onQuantityChange(item.skuId, item.quantity - 1) },
                        onIncrease = { onQuantityChange(item.skuId, item.quantity + 1) },
                        onRemove = { onRemove(item.skuId) },
                        onOpenProduct = { onOpenProduct(item.productId) },
                    )
                }
            }

            Surface(
                color = PlantupiaColors.softSage,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Text("YOUR SAVED COLLECTION", style = PlantupiaText.sectionLabel)
                    HorizontalDivider(color = PlantupiaColors.border)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Items subtotal", style = PlantupiaText.body)
                        Text(
                            "EGP " + cart.optString("subtotal"),
                            style = PlantupiaText.editorialHeading,
                        )
                    }
                    Text(
                        "Delivery is not included. Plantupia will quote its cost before you decide whether to confirm. No payment or stock reservation happens here.",
                        style = PlantupiaText.supporting,
                    )
                    if (!cart.optBoolean("all_items_available") || incomplete) {
                        Text(
                            "Review the unavailable items before asking for delivery.",
                            color = PlantupiaColors.errorText, style = PlantupiaText.supporting,
                        )
                    }
                    Button(
                        onClick = onDeliveryDetails,
                        enabled = !isActionBlocked && !incomplete &&
                            cart.optBoolean("can_request_shipping_quote"),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                    ) { Text("Continue to delivery details  ↗") }
                }
            }
        }
    }
}

@Composable
private fun PlantupiaCartObjectCard(
    item: ShopCartObject,
    index: Int,
    repository: PlantupiaRepository,
    pending: Boolean,
    enabled: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onRemove: () -> Unit,
    onOpenProduct: () -> Unit,
) {
    Surface(
        color = PlantupiaColors.paper,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, PlantupiaColors.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                BoxWithConstraints {
                    val size = if (maxWidth >= 370.dp) 110.dp else 92.dp
                    Surface(
                        onClick = onOpenProduct,
                        enabled = enabled,
                        modifier = Modifier.width(size).height(111.dp),
                        shape = RoundedCornerShape(9.dp),
                        color = PlantupiaColors.softSage,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (item.photo != null) {
                                PlantupiaCatalogPhoto(
                                    productId = item.productId,
                                    repository = repository,
                                    photoId = item.photo.id,
                                    photoAlt = item.photo.description.ifBlank { item.name },
                                    catalogPath = "commerce/catalog", fullObject = true,
                                    displayHeight = 106.dp,
                                )
                            } else {
                                PlantupiaShopObjectAwaitingPhoto(item.kind, height = 106.dp)
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("OBJECT / " + index.toString().padStart(2, '0'),
                        style = PlantupiaText.sectionLabel)
                    TextButton(
                        onClick = onOpenProduct,
                        enabled = enabled,
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(item.name.ifBlank { "Saved object" },
                            style = PlantupiaText.body, fontWeight = FontWeight.SemiBold,
                            maxLines = 3, overflow = TextOverflow.Ellipsis,
                            color = PlantupiaColors.forest)
                    }
                    if (item.variant.isNotBlank()) {
                        Text(item.variant, style = PlantupiaText.supporting)
                    }
                    if (item.skuCode.isNotBlank()) {
                        Text("SKU / " + item.skuCode, style = PlantupiaText.sectionLabel)
                    }
                    if (item.unitPrice.isNotBlank()) {
                        Text("EGP " + item.unitPrice + " each",
                            style = PlantupiaText.supporting)
                    }
                }
            }

            if (!item.lineAvailable) {
                Text(
                    item.notice.ifBlank {
                        "This option or quantity is not currently available. Review or remove it."
                    },
                    color = PlantupiaColors.errorText, style = PlantupiaText.supporting,
                )
            }
            HorizontalDivider(color = PlantupiaColors.border)

            // Independent, full-width controls survive narrow screens and large text.
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("QUANTITY", style = PlantupiaText.sectionLabel)
                    if (pending) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                            CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp,
                                color = PlantupiaColors.forest)
                            Text("Saving…", style = PlantupiaText.supporting)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { if (item.quantity <= 1) onRemove() else onDecrease() },
                        enabled = enabled && item.quantity > 0,
                        modifier = Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
                            .semantics {
                                contentDescription = if (item.quantity == 1)
                                    "Remove the last unit of " + item.name
                                else "Decrease quantity of " + item.name
                            },
                        contentPadding = PaddingValues(horizontal = 9.dp),
                    ) { Text("−") }
                    Text(item.quantity.toString(),
                        modifier = Modifier.widthIn(min = 32.dp),
                        style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(
                        onClick = onIncrease,
                        enabled = enabled && shopCartCanIncrease(item),
                        modifier = Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp)
                            .semantics { contentDescription = "Increase quantity of " + item.name },
                        contentPadding = PaddingValues(horizontal = 9.dp),
                    ) { Text("+") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onRemove, enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Remove", color = PlantupiaColors.forest)
                    }
                }
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("LINE TOTAL", style = PlantupiaText.sectionLabel)
                    Text("EGP " + item.lineTotal,
                        style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
