package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.data.PlantupiaRepository
import org.json.JSONObject

/**
 * A receipt is only made from an authenticated cart response that contains the
 * exact SKU and exact requested quantity. It is NOT an optimistic animation,
 * an inventory reservation, or evidence of an order/payment.
 */
internal data class ShopCartReceipt(
    val productId: String,
    val skuId: String,
    val productName: String,
    val optionName: String,
    val skuCode: String,
    val quantity: Int,
    val price: String,
    val lineTotal: String,
    val cartSubtotal: String,
    val photoId: String?,
    val hasPhoto: Boolean,
    val photoAlt: String,
)

internal fun shopCartReceiptFromResponse(
    cartResponse: JSONObject,
    requestedSkuId: String,
    requestedQuantity: Int,
): ShopCartReceipt? {
    if (requestedSkuId.isBlank() || requestedQuantity !in 1..10) return null
    val lines = cartResponse.optJSONArray("results") ?: return null
    for (index in 0 until lines.length()) {
        val line = lines.optJSONObject(index) ?: continue
        val sku = line.optJSONObject("product") ?: continue
        if (sku.optString("id") != requestedSkuId || line.optInt("quantity") != requestedQuantity) continue
        val productId = sku.optString("product_id")
        val name = sku.optString("name")
        if (productId.isBlank() || name.isBlank()) return null
        val hasCover = sku.optBoolean("has_image")
        val gallery = sku.optJSONArray("photos")
        val first = gallery?.optJSONObject(0)
        val galleryId = first?.optString("id").orEmpty().takeIf { it.isNotBlank() }
        return ShopCartReceipt(
            productId = productId,
            skuId = requestedSkuId,
            productName = name,
            optionName = storeVariantName(sku),
            skuCode = sku.optString("sku"),
            quantity = requestedQuantity,
            price = sku.optString("price"),
            lineTotal = line.optString("line_total"),
            cartSubtotal = cartResponse.optString("subtotal"),
            photoId = if (hasCover) null else galleryId,
            hasPhoto = hasCover || galleryId != null,
            photoAlt = if (hasCover) sku.optString("image_description") else first?.optString("alt_text").orEmpty(),
        )
    }
    return null
}

/** Distinguish a saved cart from a reserved order, even on the confirmation UI. */
@Composable
internal fun PlantupiaCartReceiptSheet(
    receipt: ShopCartReceipt,
    repository: PlantupiaRepository,
    onContinue: () -> Unit,
    onViewCart: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onContinue,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PlantupiaColors.paper,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = {
            Box(
                Modifier.padding(vertical = 12.dp).width(38.dp).height(4.dp)
                    .background(PlantupiaColors.border, RoundedCornerShape(100)),
            )
        },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("PLANTUPIA / YOUR SELECTION", style = PlantupiaText.sectionLabel)
            Column(
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("Saved to your cart", style = PlantupiaText.editorialHeading)
                Text("Your selected option is now in your saved cart.", style = PlantupiaText.supporting)
            }
            HorizontalDivider(color = PlantupiaColors.border)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(width = 90.dp, height = 95.dp)
                        .background(PlantupiaColors.softSage, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (receipt.hasPhoto) {
                        PlantupiaCatalogPhoto(
                            productId = receipt.productId,
                            repository = repository,
                            photoId = receipt.photoId,
                            photoAlt = receipt.photoAlt,
                            catalogPath = "commerce/catalog",
                            fullObject = true,
                            displayHeight = 88.dp,
                        )
                    } else {
                        PlantupiaShopObjectAwaitingPhoto("object", height = 90.dp)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(receipt.productName, style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                    if (receipt.optionName.isNotBlank()) {
                        Text(receipt.optionName, style = PlantupiaText.supporting)
                    }
                    Text("Quantity in cart · " + receipt.quantity, style = PlantupiaText.supporting)
                    if (receipt.lineTotal.isNotBlank()) {
                        Text("EGP " + receipt.lineTotal, style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Surface(
                color = PlantupiaColors.softSage,
                shape = RoundedCornerShape(11.dp),
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (receipt.cartSubtotal.isNotBlank()) {
                        Text("Saved cart subtotal · EGP " + receipt.cartSubtotal,
                            style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                    }
                    Text("No stock is reserved yet. Delivery is quoted and confirmed separately.",
                        style = PlantupiaText.supporting)
                }
            }
            Button(
                onClick = onViewCart,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
            ) { Text("View your cart  ↗") }
            OutlinedButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) { Text("Continue exploring") }
            Spacer(Modifier.height(8.dp))
        }
    }
}
